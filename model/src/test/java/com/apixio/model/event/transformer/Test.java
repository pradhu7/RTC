package com.apixio.model.event.transformer;

public class Test
{
    public static String daddyAwesomeFunction(int i, int k) {
        if(i+k == 15) {
            return "BooBoo!";
        }
        else if (i+k==30) {
            return "HaHa!";
        }
        else {
            for(int x=1; x<=i+k;x++)
            {
                System.out.println("awesome awesome! " + x);
            }
            return "";
        }
    }


    public static void main(String [] args) {
        System.out.println(daddyAwesomeFunction(2, 15));
        //System.out.println(daddyAwesomeFunction(2, 28));
        //daddyAwesomeFunction(5, 0);
    }
}
