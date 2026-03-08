import java.util.*;
import java.io.*;

public class phase1{
    //4 registers in cpu
    public static char[] R = new char[4];
    public static char[] IR = new char[4];
    public static int IC;
    public static boolean C;
    public static int SI;

    //memory
    public static char[][] mem = new char[100][4];
    
    public static int numinstr; //no. of instrs in prog card

    public static char[] buffer = new char[40];
    public static Scanner sc;
    
    public static void main(String[] args) throws Exception{
        load();
    }

    public static void load() throws Exception{
        String line, word;
        int lineptr = 0;
        int instcnt = 0;
        File f = new File("input.txt");
        sc = new Scanner(f);

        while(sc.hasNextLine()){
            line = sc.nextLine();
            if(line.length()>4) word = line.substring(0,4);
            else word = line;

            if(word.equals("$AMJ")){
                lineptr = 0; instcnt = 0;
                numinstr = Integer.parseInt(line.substring(8,12));
                init();
            }
            else if(word.equals("$DTA")) executeprog();
            else if(word.equals("$END")) {displaymem(); continue;}
            else{
                line = line.trim();
                char[] instructions = line.toCharArray();

                for(int i = 0; i<instructions.length; i++) buffer[i] = instructions[i];

                for(int i = 0; i<instructions.length; i++){
                    if(instcnt != 0 && instcnt%4 == 0) lineptr++;

                    if(instructions[i] == 'H') {mem[lineptr][0] = 'H'; instcnt = ((instcnt/4)+1)*4;}
                    else {mem[lineptr][instcnt%4] = buffer[i]; instcnt++;}
                }

                for(int i = 0; i<40; i++) buffer[i] = '-';
            }
        }
    }

    public static void init(){
        IC = 0;
        C = false;
        SI = 0;
        R[0] = 0; R[1] = 0; R[2] = 0; R[3] = 0;
        IR[0] = 0; IR[1] = 0; IR[2] = 0; IR[3] = 0;
        
        for(int i = 0; i<100; i++) {mem[i][0] = '-'; mem[i][1] = '-'; mem[i][2] = '-'; mem[i][3] = '-';}

        for(int i = 0; i<40; i++) buffer[i] = '-';
    }

    public static void executeprog(){
        int addr;
        String temp;
        try {
            while(true){
                temp = "";
                addr = 0;

                for(int i = 0; i<4; i++) IR[i] = mem[IC][i];

                String opcode = "" + IR[0] + IR[1];
                if (IR[0] == 'H') {
                    SI = 3; IC++;
                    mastermode(sc);
                    break;
                }
                
                switch(opcode){
                    case "GD": 
                        SI = 1; IC++;
                        mastermode(sc);
                        break;

                    case "PD": 
                        SI = 2; IC++;
                        mastermode(sc);
                        break; 

                    case "LR":
                        IC++;
                        temp = "" + IR[2] + IR[3];
                        addr = Integer.parseInt(temp);
                        for(int i = 0; i<4; i++) R[i] = mem[addr][i];
                        break;

                    case "SR":
                        IC++;
                        temp = "" + IR[2] + IR[3];
                        addr = Integer.parseInt(temp);
                        for(int i = 0; i<4; i++) mem[addr][i] = R[i];
                        break;

                    case "CR":
                        IC++;
                        temp = "" + IR[2] + IR[3];
                        addr = Integer.parseInt(temp);
                        for(int i = 0; i<4; i++){
                            if(R[i] == mem[addr][i]) C = true;
                            else {C = false; break;}
                        }
                        break;

                    case "BT":
                        if(C==true){
                            temp = "" + IR[2] + IR[3];
                            addr = Integer.parseInt(temp);
                            IC = addr;
                        }
                        else IC++;
                        break;

                    default: System.out.println("Invalid instruction detected: " + opcode + "\nExiting.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void mastermode(Scanner sc) throws Exception{
        switch(SI){
            case 1: read(sc); break;
            case 2: write(); break;
            case 3: terminate(); break;
            default: System.out.println("Invalid system interrupt: " + SI + "\nExiting.");
        }
    }

    public static void read(Scanner sc){
        String data = sc.nextLine();
        char[] chars = data.toCharArray();
        int len = data.length();
        int loc = Integer.parseInt(""+IR[2]+IR[3]);
        int i = 0, m = 0;

        while(i<len){
            if(i%4 == 0 && m!=0) loc++;
            mem[loc][m%4] = chars[i];
            i++; m++;
        }
    }

    public static void write() throws Exception{
        int loc = Integer.parseInt(""+IR[2]+IR[3]);
        String data = "";

        for(int i = loc; i<loc+10; i++){
            for(int j = 0; j<4; j++){
                if(mem[i][j] != '-') data += mem[i][j];
                else data += ' ';
            }
        }

        FileWriter fw = new FileWriter("output.txt", true);
        data = data.trim() + "\n";
        fw.write(data);
        fw.close();
    }

    public static void displaymem(){
		for(int i=0; i<100; i++){
			if(i%10==0 && i!=0)
				System.out.println("-----------------------------------------------------------------");
			System.out.println(i+"	"+mem[i][0]+mem[i][1]+mem[i][2]+mem[i][3]);
		}
		System.out.println();
		System.out.println();
		System.out.println();
    }

    public static void terminate() throws Exception{
        FileWriter fw = new FileWriter("output.txt", true);
        fw.write("\n\n");
        fw.close();
    }
}
