import java.util.*;
import java.io.*;

public class phase2{
    public static char[] R = new char[4];
    public static char[] IR = new char[4];
    public static int IC;
    public static boolean C;
    public static int PTR;

    public static char[][] mem = new char[300][4];
    public static char[] buffer = new char[40];

    public static int SI;
    public static int PI;
    public static int TI;
    
    public static int numinstr;
    public static int TLC;
    public static int TTL;
    public static int LLC;
    public static int TLL;

    public static int gdsrcall; // to keep track of if page fault was called by gd or sr (gd = 1, sr = 2, else = 0)
    public static int framevalid; // to keep track of the frame assigned during valid pg fault
    public static ArrayList<Integer> framenums = new ArrayList<Integer>();
    public static int frameidx;
    public static int pageno;
    public static int PTE;
    public static boolean lle; // LLE error
    public static boolean ood; // out of data error

    static pcb PCB = new pcb();
    public static Scanner sc;
    
    public static void main(String[] args) throws Exception{
        load();
    }

    public static void load() throws Exception{
        String line, word;
        int lineptr = 0;
        int instcnt = 0;
        pageno = 49;
        int frametofill;

        File f = new File("inputE.txt");
        sc = new Scanner(f);

        while(sc.hasNextLine()){
            line = sc.nextLine();
            if(line.length()>4) word = line.substring(0,4);
            else word = line;

            if(word.equals("$AMJ")){
                lineptr = 0; instcnt = 0;
                PCB.pid = Integer.parseInt(line.substring(4, 8));
                TTL = Integer.parseInt(line.substring(8, 12));
                PCB.ttl = TTL;
                TLL = Integer.parseInt(line.substring(12));
                PCB.tll = TLL;
                init();
            }
            else if(word.equals("$DTA")) executeprog();
            else if(word.equals("$END")) {displaymem(); continue;}
            else{
                line = line.trim();
                char[] instructions = line.toCharArray();

                for(int i = 0; i<instructions.length; i++) buffer[i] = instructions[i];

                int pterow = PTR;
                frametofill = allocate();
                mem[pterow][0] = 'p';
                mem[pterow][1] = (char)pageno;
                String framebeingfilled = Integer.toString(frametofill);
                if(frametofill<10) framebeingfilled = "0" + framebeingfilled;
                mem[pterow][2] = framebeingfilled.charAt(0);
                mem[pterow][3] = framebeingfilled.charAt(1);
                pterow++;
                lineptr = frametofill*10;


                for(int i = 0; i<instructions.length; i++){
                    if(instcnt != 0 && instcnt%4 == 0) lineptr++;

                    if(instructions[i] == 'H') {mem[lineptr][0] = 'H'; numinstr++; instcnt = ((instcnt/4)+1)*4;}
                    else {mem[lineptr][instcnt%4] = buffer[i]; instcnt++; if(instcnt%4 == 0) numinstr++;}
                }

                for(int i = 0; i<40; i++) buffer[i] = '-';
            }
        }
    }

    public static int allocate(){
        int n = framenums.get(frameidx);
        frameidx++;
        return n;
    }

    public static void init(){
        IC = 0;
        C = false;
        SI = 0; PI = 0; TI = 0;
        lle = false; ood = false;
        pageno = 49; frameidx = 0;
        numinstr = 0;
        TLC = 0; TTL = 0; LLC = 0; TLL = 0;
        gdsrcall = 0; framevalid = 0;
        R[0] = '-'; R[1] = '-'; R[2] = '-'; R[3] = '-';
        IR[0] = '-'; IR[1] = '-'; IR[2] = '-'; IR[3] = '-';
        
        for(int i = 0; i<300; i++) {mem[i][0] = '-'; mem[i][1] = '-'; mem[i][2] = '-'; mem[i][3] = '-';}

        for(int i = 0; i<40; i++) buffer[i] = '-';

        framenums.clear();
        for(int i = 0; i<30; i++) framenums.add(i);
        Collections.shuffle(framenums); //shuffled list of nums so that we dont need to call random everytime
        PTR = allocate();
    }

    public static int addresstranslate(String virtualaddr, Scanner sc) throws Exception{
        int VA = 0;
        try{
            VA = Integer.parseInt(virtualaddr);
        } catch(Exception e){
            PI = 2;
            mastermode(sc);
        }

        PTE = PTR + VA/10;

        if(mem[PTE][0] != 'p'){
            PI = 3;
            mastermode(sc);
            if(PI!=3) {int RA = framevalid*10 + VA%10; return RA;}
            else return -1;
        }
        else{
            String temp=""+mem[PTE][2]+mem[PTE][3];
            int frame = Integer.parseInt(temp);
            int RA = frame*10 + VA%10;
            return RA;
        }
    }

    public static void executeprog(){
        IC = 0;
        int VA, RA;
        String temp;
        try {
            while(IC!=numinstr){
                PCB.tlc = TLC;
                if(TLC>TTL) {TI=2; mastermode(sc);}
                temp = "";
                String tempra = "" + IC;
                RA = addresstranslate(tempra, sc);
                VA = 0;

                for(int i = 0; i<4; i++) IR[i] = mem[RA][i];

                String opcode = "" + IR[0] + IR[1];
                if (IR[0] == 'H') {
                    SI = 3; IC++;
                    mastermode(sc);
                    TLC++;
                    break;
                }
                
                switch(opcode){
                    case "GD": 
                        SI = 1; IC++;
                        mastermode(sc);
                        TLC++;
                        break;

                    case "PD": 
                        SI = 2; IC++;
                        mastermode(sc);
                        TLC++;
                        break; 

                    case "LR":
                        IC++;
                        temp = "" + IR[2] + IR[3];
                        VA = addresstranslate(temp, sc);
                        for(int i = 0; i<4; i++) R[i] = mem[VA][i];
                        TLC++;
                        break;

                    case "SR":
                        IC++;
                        temp = "" + IR[2] + IR[3];
                        VA = addresstranslate(temp, sc);
                        for(int i = 0; i<4; i++) mem[VA][i] = R[i];
                        gdsrcall = 2;
                        TLC++;
                        break;

                    case "CR":
                        IC++;
                        temp = "" + IR[2] + IR[3];
                        VA = addresstranslate(temp, sc);
                        for(int i = 0; i<4; i++){
                            if(R[i] == mem[VA][i]) C = true;
                            else {C = false; break;}
                        }
                        TLC++;
                        break;

                    case "BT":
                        if(C==true){
                            temp = "" + IR[2] + IR[3];
                            VA = addresstranslate(temp, sc);
                            IC = VA;
                        }
                        else IC++;
                        TLC++;
                        break;

                    default:{PI=1; mastermode(sc);}
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void mastermode(Scanner sc) throws Exception{
        if(TI==0 && SI==1){SI=0; read(sc);}
        else if(TI==0 && SI==2){SI=0; write(sc);}
        else if(TI==0 && SI==3){terminate("Process exited with status 0",sc);}
        else if(TI==2 && SI==1){terminate("Time Limit Exceeded",sc);}
        else if(TI==2 && SI==2){write(sc); terminate("Time Limit Exceeded",sc);}
        else if(TI==2 && SI==3){terminate("Process exited with status 0",sc);}

        else if(TI==0 && PI==1){terminate("Opcode Error",sc);}
        else if(TI==0 && PI==2){terminate("Operand Error",sc);}
        else if(TI==0 && PI==3){
            if(gdsrcall==0) terminate("Invalid Page Fault",sc);
            else{
                gdsrcall=0;
                int frametofill=allocate();
                mem[PTE][0] = 'p';
                mem[PTE][1] = (char)pageno;
                String framebeingfilled = Integer.toString(frametofill);
                if(frametofill<10) framebeingfilled = "0" + framebeingfilled;
                mem[PTE][2] = framebeingfilled.charAt(0);
                mem[PTE][3] = framebeingfilled.charAt(1);
                framevalid = frametofill;
                TLC++;
                PI=0;
            }
        }
        else if(TI==2 && PI==1){terminate("Time Limit Exceeded\nOpcode Error",sc);}
        else if(TI==2 && PI==2){terminate("Time Limit Exceeded\nOperand Error",sc);}
        else if(TI==2 && PI==3){terminate("Time Limit Exceeded\nInvalid Page Fault",sc);}
    }

    public static void read(Scanner sc) throws Exception{
        String data = sc.nextLine();
        String subdata;
        if(data.length()>4) subdata=data.substring(0,4);
        else subdata=data;

        if(subdata.equals("$END")){ood=true; terminate("Out of data",sc);}
        else{
            char[] datachar=data.toCharArray();
            int length = datachar.length;
            String temp = ""+IR[2]+IR[3];
            gdsrcall = 1;
            int block = addresstranslate(temp, sc);
            int arraycnt=0;
            int memcnt=0;
            while(arraycnt!=length){
                if(datachar[arraycnt] == ' ' && memcnt!=0){
                    block++;
                    arraycnt++;
                    memcnt=((memcnt/4)+1)*4;
                }
                else if(memcnt%4==0 && memcnt!=0) block++;

                mem[block][memcnt%4]=datachar[arraycnt];
                arraycnt++;
                memcnt++;
            }
        }
    }

    public static void write(Scanner sc) throws Exception{
        LLC++;
        PCB.llc=LLC;
        if(LLC>TLL){lle=true; terminate("Line Limit Exceeded",sc);}
        else{
            String temp=""+IR[2]+IR[3];
            int block=addresstranslate(temp,sc);
            if(block!=-1 && !ood){
                int i,j;
                String data="";
                for(i=block; i<block+10; i++){
                    for(j=0; j<4; j++) if(mem[i][j]!='-') data=data+mem[i][j];
                    if(mem[i][j-1]=='-') data=data+" ";
                }

                FileWriter fw = new FileWriter("output.txt",true);
                data = data + "\n";
                fw.write(data);
                fw.close();
            }
        }
    }

    public static void displaymem(){
		int i;
        System.out.println();
        for(i=0; i<300; i++){
            if(i%10==0 && i!=0) System.out.println("-----------------------------------------------------------------");
            System.out.println(i+"  "+mem[i][0]+mem[i][1]+mem[i][2]+mem[i][3]);
        }
        System.out.println();
        System.out.println();
        System.out.println("===========PCB==========");
        System.out.println("Process ID : "+ PCB.pid);
        System.out.println("Total Time Limit : "+ PCB.ttl);
        System.out.println("Total Line Limit : "+ PCB.tll);
        System.out.println("Time Given : "+ PCB.tlc);
        System.out.println("Lines Printed : "+ PCB.llc);
        System.out.println("\n\n\n\n\n\n");
    }

    public static void terminate(String message, Scanner sc) throws Exception{
        FileWriter fw=new FileWriter("output.txt",true);
        String temp="SI : "+SI+"  PI : "+PI+"  TI : "+TI;
		
        if(ood){fw.write(message+"\n"+temp+"\n\n\n"); IC = numinstr;}
		else if(lle || PI!=0 || TI==2){
            fw.write(message+"\n"+temp+"\n\n\n");
            String card;
            String subcard;
            do{
                card=sc.nextLine();
                if(card.length()>4)
                        subcard=card.substring(0,4);
                else
                        subcard=card;
                IC = numinstr;
            }while(!subcard.equals("$END"));
        }
        else fw.write(message+"\n"+temp+"\n\n\n");
        fw.close();
    }
}
