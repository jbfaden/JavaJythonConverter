
class LongestArithmeticProgression {
// a method for finding the larger number between i & j  

    public int findMaxNo(int i, int j) {
        int maxNo;
        return maxNo = (i > j) ? i : j;
    }

    public int findLongestApSeq(int arr[], int size) {

// two or less than two elements always form  
// the arithmetic progression  
        if (size <= 2) {
            return size;
        }

        int ans = -1;

// loop for picking the first element of the  
// arithemtic progression  
        for (int i = 0; i < size - 2; i++) {

// loop for picking the second element of the  
// arithemtic progression  
            for (int j = i + 1; j < size - 1; j++) {
// finding the common difference  
                int comDiff = arr[j] - arr[i];

// multiple is 2 becuase the third element  
// is first element + 2 * common difference  
                int multiple = 2;

// the first two loops has fixed the  
// first two elements of the arithmetic progresssion  
                int countEle = 2;

// loop for picking the subsequent elements (3rd, 4th, 5th, ..., so on)  
// of the arithmetic progression  
                for (int k = j + 1; k < size; k++) {
                    if ((arr[i] + multiple * comDiff) == arr[k]) {
// element found hence increment the count  
                        countEle = countEle + 1;

// 4th element of an arithmetic progression  
// is defined as: a[i] + 3 * common difference  
                        multiple = multiple + 1;
                    }
                }

                ans = findMaxNo(ans, countEle);

            }
        }

        return ans;
    }
// main method  

    public static void main(String argvs[]) {
// creating an object of the class LongestArithmeticProgression  
        LongestArithmeticProgression obj = new LongestArithmeticProgression();

// input array  
        int inputArr[] = {30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140};

        int size = inputArr.length;

        int longestSeq = obj.findLongestApSeq(inputArr, size);

        System.out.println("For the following array: ");
        for (int k = 0; k < size; k++) {
            System.out.print(inputArr[k] + " ");
        }
        System.out.println("\n");
        System.out.println("The length of the longest arithmetic progression sequence is: " + longestSeq);

        int inputArr1[] = {15, 7, 20, 9, 14, 15, 25, 30, 90, 100, 35, 40};

        size = inputArr1.length;

        longestSeq = obj.findLongestApSeq(inputArr1, size);

        System.out.println("\n");

        System.out.println("For the following array: ");
        for (int k = 0; k < size; k++) {
            System.out.print(inputArr1[k] + " ");
        }
        System.out.println("\n");
        System.out.println("The length of the longest arithmetic progression sequence is: " + longestSeq);

    }
}
