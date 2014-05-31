#include <iostream>
#include <vector>
#include <omp.h>
using namespace std;

int main () {
    int k;
    k = 10;
    #pragma omp parallel default(shared) private(k)
    {
        k = k + 3;
        #pragma omp barrier
        cout << k;
    }
}


