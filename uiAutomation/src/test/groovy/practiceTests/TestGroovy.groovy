package practiceTests

import org.testng.annotations.Test

import java.awt.Point

class TestGroovy {

    @Test
    void testInstance(){
        def value = "dsf"
        print(value.class.simpleName)
    }

    @Test
    void hasSum(){
        int sum = 8
        int[] array = [1,2,4,4,6,5,3]

        for (int i=0;i<array.length-1;i++){
            for (int j=i+1;j<array.length;j++){
                if (array[i]+array[j]==sum){
                    print("the pair is ${array[i]}, ${array[j]}")
                }
            }
        }
    }
}
