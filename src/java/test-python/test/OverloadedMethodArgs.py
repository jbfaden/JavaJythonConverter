import sys


def printSum(nn, fill=0):
    sum = 0
    for i in range(0, len(nn)):
        if nn[i] != fill:
            sum += nn[i]
    sys.stderr.write('Sum is ' + str(sum)+'\n')


def main(args):
    printSum([1, 2, 3, 4, 5])


if __name__ == '__main__':
    main([])

