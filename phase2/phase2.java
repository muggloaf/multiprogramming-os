import java.util.*;
import java.io.*;

public class phase2{
    //4 registers in cpu
    public static char[] R = new char[4];
    public static char[] IR = new char[4];
    public static int IC;
    public static boolean C;
    public static int SI;
    public static int TI;
    public static int PI;

    //memory
    public static char[][] mem = new char[300][4];

    public static char[] buffer = new char[40];
    public static char[] PTR=new char[4];

    public static int numinstr; //no. of instrs in prog card
    public static int TLC;
    public static int TTL;
    public static int LLC;
    public static int TLL;
    public static int gdcall;
    public static int srcall;
    public static int framevpf; //frame assigned during valid pg fault
    public static ArrayList<Integer> framenums=new ArrayList<Integer>();
    public static int frameidx;
    public static int pagenum;
    public static int pgtableloc; //row in page table where we need to make an entry
    public static int pagetableAT; //pte after valid pg fault is handled
    public static int LLE; //line limit exceeded
    public static int OOD; //out of data error

    static PCB pro = new PCB();
    public static Scanner sc;
    
    public static void main(String[] args) throws Exception{
        load();
    }

    public static void load() throws Exception{
        String line, word;
        int lineptr = 0;
        int instcnt = 0;
        pagenum=49;
        int frametofill;
        File f = new File("inputNE.txt");
        sc = new Scanner(f);

        while(sc.hasNextLine()){
            line = sc.nextLine();
            if(line.length()>4) word = line.substring(0,4);
            else word = line;

            if(word.equals("$AMJ")){
                lineptr = 0; instcnt = 0;
                numinstr = Integer.parseInt(line.substring(8,12));
                pro.pid=Integer.parseInt(line.substring(4,8));
                TTL=Integer.parseInt(line.substring(8,12));
                pro.TTL=TTL;
                TLL=Integer.parseInt(line.substring(12));
                pro.TLL=TLL;
                init();
            }
            else if(word.equals("$DTA")) {pro.currentstat="EXECUTE"; executeprog();}
            else if(word.equals("$END")) {pro.currentstat="FINISHED"; displaymem(); continue;}
            else{
                pro.currentstat="FETCH";
                instcnt = 0;
                line = line.trim();
                char[] instructions = line.toCharArray();

                for(int i = 0; i<instructions.length; i++) buffer[i] = instructions[i];

                frametofill = allocate();
                String idk = Integer.toString(frametofill);
                if(frametofill<10) idk = "0"+idk;
                char[] frameAsChar = idk.toCharArray();
                mem[pgtableloc][0] = 'P';
                mem[pgtableloc][1] = (char)pagenum;
                pagenum++;
                mem[pgtableloc][2] = frameAsChar[0];
                mem[pgtableloc][3] = frameAsChar[1];
                pgtableloc++;
                lineptr = frametofill*10;

                for(int i = 0; i<instructions.length; i++){
                    if(instcnt!=0 && instcnt%4==0){
                        lineptr++;
                        numinstr++;
                    }
                    if(instructions[i]=='H'){
                        mem[lineptr][instcnt%4]=buffer[i];
                        numinstr++;
                        instcnt=((instcnt/4)+1)*4;
                    }
                    else{
                        mem[lineptr][instcnt%4]=buffer[i];
                        instcnt++;
                        if(i==(instructions.length-1)) numinstr++;
                    }
                }

                for(int i = 0; i<40; i++) buffer[i] = '-';
            }
        }
    }

    public static int allocate(){
        int n=framenums.get(frameidx);
        frameidx++;
        return n;
    }

    public static void init() throws Exception{
        IC = 0;
        C = false;
        SI = 0; TI=0; PI=0;
        TLC=0; LLC=0;
        pagenum=49;
        numinstr = 0;
        LLE=0; OOD=0;
        gdcall=0; srcall=0;
        framevpf=0; frameidx=0;

        R[0] = 0; R[1] = 0; R[2] = 0; R[3] = 0;
        IR[0] = 0; IR[1] = 0; IR[2] = 0; IR[3] = 0;

        framenums.clear();
        for(int i=0; i<30; i++) framenums.add(i);
        Collections.shuffle(framenums);
        
        int temp=allocate();
        pgtableloc=temp*10;
        String frame=Integer.toString(temp);
        if(temp<10) frame="00"+frame+"0";
        else frame="0"+frame+"0";
        char[] r=frame.toCharArray();

        PTR[0]=r[0];
        PTR[1]=r[1];
        PTR[2]=r[2];
        PTR[3]=r[3];
        System.out.println("PTR : "+PTR[0]+PTR[1]+PTR[2]+PTR[3]);
        
        for(int i = 0; i<300; i++) {mem[i][0] = '-'; mem[i][1] = '-'; mem[i][2] = '-'; mem[i][3] = '-';}

        for(int i = 0; i<40; i++) buffer[i] = '-';

        File of=new File("output.txt");
        if(of.createNewFile())
        System.out.println("Created an output file : output.txt");
    }

    public static int addresstranslate(String va, Scanner sc) throws Exception{
        int VA = 0;
        try{
            VA=Integer.parseInt(va);
            System.out.println("Virtual address: "+VA+" Thingy we passed: "+va);
        }
        catch(Exception e){
            PI=2;
            mastermode(sc);
        }

        String pageTable=""+PTR[0]+PTR[1]+PTR[2]+PTR[3];
        int ptrval=Integer.parseInt(pageTable);
        int pte=ptrval+(VA/10);
        pagetableAT=pte;

        if(mem[pte][0]!='P'){
            PI=3;
            mastermode(sc);
            if(PI!=3){
                int RA=framevpf*10;
                RA=RA+VA%10;
                return RA;
            }
            else return -1;
        }
        else{
            String temp=""+mem[pte][2]+mem[pte][3];
            int framenum=Integer.parseInt(temp);
            int RA=(framenum*10)+(VA%10);
            srcall = 0;
            return RA;
        }
    }

    public static void executeprog(){
        int addr, RA;;
        String temp;
        try {
            while(IC!=numinstr){
                pro.completiontime = TLC;
                System.out.println("Value of TLC: "+TLC);
                if(TLC>=TTL) TI = 2;
                System.out.println("\n\nValue of IC : "+IC);

                temp = "";
                addr = 0;

                String temp2 = "" + IC;
                RA = addresstranslate(temp2,sc);
                System.out.println("Real Address corresponding to IC "+IC+" is : "+RA);

                for(int i = 0; i<4; i++) IR[i] = mem[RA][i];

                String opcode = "" + IR[0] + IR[1];
                if (IR[0] == 'H') {
                    System.out.println(""+IR[0]+IR[1]+IR[2]+IR[3]);
                    IC++; SI=3;
                    mastermode(sc);
                    TLC++;
                    break;
                }
                
                switch(opcode){
                    case "GD": 
                        System.out.println(""+IR[0]+IR[1]+IR[2]+IR[3]);
                        IC++; SI=1;
                        mastermode(sc);
                        TLC++;
                        break;

                    case "PD": 
                        System.out.println(""+IR[0]+IR[1]+IR[2]+IR[3]);
                        IC++; SI=2;
                        mastermode(sc);
                        TLC++;
                        break; 

                    case "LR":
                        System.out.println(""+IR[0]+IR[1]+IR[2]+IR[3]);
                        IC++;
                        temp = "" + IR[2] + IR[3];
                        addr = Integer.parseInt(temp);
                        addr = addresstranslate(temp,sc);
                        for(int i = 0; i<4; i++) R[i] = mem[addr][i];
                        TLC++;
                        break;

                    case "SR":
                        System.out.println(""+IR[0]+IR[1]+IR[2]+IR[3]);
                        IC++;
                        temp = "" + IR[2] + IR[3];
                        addr = Integer.parseInt(temp);
                        srcall = 1;
                        addr = addresstranslate(temp,sc);
                        for(int i = 0; i<4; i++) mem[addr][i] = R[i];
                        TLC++;
                        break;

                    case "CR":
                        System.out.println(""+IR[0]+IR[1]+IR[2]+IR[3]);
                        IC++;
                        temp = "" + IR[2] + IR[3];
                        addr = Integer.parseInt(temp);
                        addr = addresstranslate(temp,sc);
                        for(int i = 0; i<4; i++){
                            if(R[i] == mem[addr][i]) C = true;
                            else {C = false; break;}
                        }
                        TLC++;
                        break;

                    case "BT":
                        System.out.println(""+IR[0]+IR[1]+IR[2]+IR[3]);
                        if(C==true){
                            temp = "" + IR[2] + IR[3];
                            addr = Integer.parseInt(temp);
                            IC = addr;
                        }
                        else IC++;
                        TLC++;
                        break;

                    default: {
                        System.out.println(""+IR[0]+IR[1]+IR[2]+IR[3]);
                        System.out.println("No opcode matches");
                        PI = 1;
                        mastermode(sc);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void mastermode(Scanner sc) throws Exception{
        System.out.println("Entered masterMode()");
        System.out.println("SI : "+SI);
        System.out.println("TI : "+TI);
        System.out.println("PI : "+PI);

        if(TLC>=TTL) TI=2;
        if(TI==0 && SI==1){SI=0; read(sc);}
        else if(TI==0 && SI==2){SI=0; write(sc);}
        else if(TI==0 && SI==3) terminate("Process exited with status 0",sc);
        else if(TI==2 && SI==1) terminate("Time Limit Exceeded",sc);
        else if(TI==2 && SI==2){write(sc); terminate("Time Limit Exceeded",sc);}
        else if(TI==2 && SI==3) terminate("Process exited with status 0",sc);

        else if(TI==0 && PI==1) terminate("Opcode Error",sc);
        else if(TI==0 && PI==2) terminate("Operand Error",sc);
        else if(TI==0 && PI==3){
            if(gdcall==1){
                gdcall=0;
                int frametofill=allocate();
                System.out.println("Frame assigned while handling valid page fault caused by GD : "+frametofill);
                String idk=Integer.toString(frametofill);
                if(frametofill<10)
                        idk="0"+idk;
                char[] frameAsChar=idk.toCharArray();
                mem[pagetableAT][0]='P';
                mem[pagetableAT][1]=(char)pagenum;
                pagenum++;
                mem[pagetableAT][2]=frameAsChar[0];
                mem[pagetableAT][3]=frameAsChar[1];
                framevpf=frametofill;
                TLC++;
                PI=0;
            }
            else if(srcall==1){
                srcall=0;
                System.out.println("SR was reset");
                int frametofill=allocate();
                System.out.println("Frame assigned while handling valid page fault caused by SR : "+frametofill);
                String idk=Integer.toString(frametofill);
                if(frametofill<10)
                        idk="0"+idk;
                char[] frameAsChar=idk.toCharArray();
                mem[pagetableAT][0]='P';
                mem[pagetableAT][1]=(char)pagenum;
                pagenum++;
                mem[pagetableAT][2]=frameAsChar[0];
                mem[pagetableAT][3]=frameAsChar[1];
                framevpf=frametofill;
                TLC++;
                PI=0;
            }
            else terminate("Invalid Page Fault",sc);
        }

        else if(TI==2 && PI==1) terminate("Time Limit Exceeded\nOpcode Error",sc);
        else if(TI==2 && PI==2) terminate("Time Limit Exceeded\nOperand Error",sc);
        else if(TI==2 && PI==3) terminate("Time Limit Exceeded\nInvalid Page Fault",sc);
    }

    public static void read(Scanner sc) throws Exception{
        String data = sc.nextLine();
        data=data.trim();
        System.out.println("Data read : "+data);
        String subdata;

        if(data.length()>4) subdata=data.substring(0,4);
        else subdata=data;

        if(subdata.equals("$END")){
            OOD=1;
            terminate("Out of data", sc);
        }
        else{
            char[] datachar = data.toCharArray();
            int length = datachar.length;
            String temp2 = ""+IR[2]+IR[3];
            gdcall = 1;
            int block = addresstranslate(temp2, sc);
            int arraycnt = 0;
            int memcnt = 0;
            while(arraycnt!=length){
                if(memcnt%4==0 && memcnt!=0) block++;
                mem[block][memcnt%4]=datachar[arraycnt];
                arraycnt++;
                memcnt++;
            }
        }
    }

    public static void write(Scanner sc) throws Exception{
        LLC++;
        pro.linesprinted=LLC;
        if(LLC>TLL){
            LLE=1;
            terminate("Line Limit Exceeded",sc);
        }
        else{
            String temp=""+IR[2]+IR[3];
            int block=addresstranslate(temp,sc);
            if(block!=-1 && OOD!=1){
                System.out.println("Writing from block : "+block+" to output file");
                int i,j;
                String data="";
                for(i=block; i<block+10; i++){
                    for(j=0; j<4; j++){
                        if(mem[i][j]!='-') data += mem[i][j];
                        else data += ' ';
                    }
                    if(mem[i][j-1]=='-') data += ' ';
                }
                FileWriter fw = new FileWriter("output.txt",true);
                data = data+"\n";
                fw.write(data);
                fw.close();
            }
        }
    }

    public static void displaymem(){
		for(int i=0; i<300; i++){
			if(i%10==0 && i!=0)
				System.out.println("-----------------------------------------------------------------");
			System.out.println(i+"	"+mem[i][0]+mem[i][1]+mem[i][2]+mem[i][3]);
		}
		System.out.println();
		System.out.println();
		System.out.println("===========PCB==========");
        System.out.println("Process ID : "+pro.pid);
        System.out.println("Total Time Limit : "+pro.TTL);
        System.out.println("Total Line Limit : "+pro.TLL);
        System.out.println("Time Given : "+pro.completiontime);
        System.out.println("Lines Printed : "+pro.linesprinted);
        System.out.println("Current Status : "+pro.currentstat);
        System.out.println("\n\n\n\n\n\n");
    }

    public static void terminate(String message, Scanner sc) throws Exception{
        FileWriter fw=new FileWriter("output.txt",true);
        String IRcontent = "" + IR[0] + IR[1] + IR[2] + IR[3];
        String ICcontent = String.valueOf(IC);
        String temp="SI : "+SI+"  PI : "+PI+"  TI : "+TI+"   IR : "+IRcontent+"   IC : "+ICcontent;
		
        if(OOD==1){
			fw.write(message+"\n"+temp+"\n\n\n");
			IC=numinstr;
		}
		else if(LLE==1 || PI!=0 || TI==2){
            fw.write(message+"\n"+temp+"\n\n\n");
            String card;
            String subcard;
            do{
                card=sc.nextLine();
                if(card.length()>4) subcard=card.substring(0,4);
                else subcard=card;
                IC = numinstr;
            }while(!subcard.equals("$END"));
        }
        else fw.write(message+"\n"+temp+"\n\n\n");
        fw.close();

        System.out.println("===========PCB==========");
        System.out.println("Process ID : "+pro.pid);
        System.out.println("Total Time Limit : "+pro.TTL);
        System.out.println("Total Line Limit : "+pro.TLL);
        System.out.println("Time Given : "+pro.completiontime);
        System.out.println("Lines Printed : "+pro.linesprinted);
        System.out.println("Current Status : "+pro.currentstat);
        System.out.println("\n\n\n\n\n\n");
    }
}
