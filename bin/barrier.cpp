#include <iostream>
#include <vector>
#include <omp.h>
using namespace std;

int test (int i) {
    i = i + 2;
    if (false) { 
        i = i + 1;
                i = 2;
    } 
    return i;
}

int test$ (int i) {
    #pragma omp critical
    i = i + 2;
    if (false) { 
        #pragma omp critical
        i = i + 1;
        #pragma omp barrier
        #pragma omp critical
        i = 2;
    } 
    return i;
}

int test_ (int i) {
    i = i + 2;
    if (false) { 
        i = i + 1;
        #pragma omp barrier
        i = 2;
    } 
    return i;
}

int main () {
    int k;
    k = 10;
    int a;
    #pragma omp parallel default(shared) firstprivate(k) private(a)
    {
        k = k + 3;
        #pragma omp barrier
        cout << k;
        #pragma omp barrier
    }
    cout << k;
    cout << "\n";
        k = 44;
    #pragma omp parallel default(shared) firstprivate(k) private(a)
    {
        cout << k;
        #pragma omp barrier
    }
    cout << k;
    cout << "\n";
}


