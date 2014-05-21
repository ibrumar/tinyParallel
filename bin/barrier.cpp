#include <iostream>
#include <vector>
#include <omp.h>
using namespace std;

int test (int i) {
    return i;
}

int test$ (int i) {
    return i;
}

int test_ (int i) {
    return i;
}

int main () {
    int k;
    k = 10;
    int a;
    #pragma omp parallel default(shared) firstprivate(k) private(a)
    {
        k = k + 77;
        cout << k;
        cout << " ";
    }
    cout << k;
    cout << "\n";
    #pragma omp parallel default(shared) firstprivate(k) private(a)
    {
        cout << k;
        cout << " ";
        k = k + 77;
    }
    cout << k;
    cout << "\n";
}


