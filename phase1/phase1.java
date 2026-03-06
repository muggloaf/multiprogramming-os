import java.util.*;
import java.io.*;

public class Phase1{
    //4 registers in cpu
    public static char[] R = new char[4];
    public static char[] IR = new char[4];
    public static int IC;
    public static bool C;
    public static int SI;

    //memory
    public static char[][] mem = new char[100][4];
    
    public static int numinstr; //no. of instrs in prog card

    public static char[] buffer = new buffer[40];
    
    public static void main(){
        load();
    }

    public static void init(){
        IC = 0;
        C = false;
        SI = 0;
        R[0] = 0, R[1] = 0, R[2] = 0, R[3] = 0;
        IR[0] = 0, IR[1] = 0, IR[2] = 0, IR[3] = 0;
        
        for(int i = 0; i<100; i++) mem[i][0] = '-', mem[i][1] = '-', mem[i][2] = '-', mem[i][3] = '-';

        for(int i = 0; i<40; i++) buffer[i] = '-';
    }
}
