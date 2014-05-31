#include <iostream>
#include <vector>
#include <omp.h>
using namespace std;

bool check (vector<int> &res, int init_val, int size) {
    int i;
    for (i = 0 ; i < size ; i = i + 1) { 
        int j;
        for (j = 0 ; j < size ; j = j + 1) { 
            if (init_val * init_val * size != res[i * size + j]) { 
                return false;
            } 
        } 
    } 
    return true;
}

bool check$ (vector<int> &res, int init_val, int size) {
    int i;
    for (i = 0 ; i < size ; i = i + 1) { 
        int j;
        for (j = 0 ; j < size ; j = j + 1) { 
            if (init_val * init_val * size != res[i * size + j]) { 
                return false;
            } 
        } 
    } 
    return true;
}

bool check_ (vector<int> &res, int init_val, int size) {
    int i;
    for (i = 0 ; i < size ; i = i + 1) { 
        int j;
        for (j = 0 ; j < size ; j = j + 1) { 
            if (init_val * init_val * size != res[i * size + j]) { 
                return false;
            } 
        } 
    } 
    return true;
}

int initialization$ (vector<int> &matA, vector<int> &matB, int size, int val) {
    int i;
        for (i = 0 ; i < size ; i = i + 1) { 
            matA[i] = val;
            matB[i] = val;
        } 
    return 0;
}

int initialization_ (vector<int> &matA, vector<int> &matB, int size, int val) {
    int i;
        for (i = 0 ; i < size ; i = i + 1) { 
            matA[i] = val;
            matB[i] = val;
        } 
    return 0;
}

int product$ (vector<int> &matA, vector<int> &matB, vector<int> &res, int size, int m) {
    int i;
    int j;
    int k;
        for (i = 0 ; i < m ; i = i + 1) { 
            for (j = 0 ; j < m ; j = j + 1) { 
                int tmp;
                tmp = 0;
                for (k = 0 ; k < m ; k = k + 1) { 
                    tmp = tmp + matA[i * m + k] * matB[k * m + j];
                } 
                res[i * m + j] = tmp;
            } 
        } 
    return 0;
}

int product_ (vector<int> &matA, vector<int> &matB, vector<int> &res, int size, int m) {
    int i;
    int j;
    int k;
        for (i = 0 ; i < m ; i = i + 1) { 
            for (j = 0 ; j < m ; j = j + 1) { 
                int tmp;
                tmp = 0;
                for (k = 0 ; k < m ; k = k + 1) { 
                    tmp = tmp + matA[i * m + k] * matB[k * m + j];
                } 
                res[i * m + j] = tmp;
            } 
        } 
    return 0;
}

int main () {
    cout << "Enter number of rows and columns \n(both matrices will be square and of the same size)\n";
    int m;
    cin >> m;
    int size;
    size = m * m;
    cout << "Enter the valor which will be used to initialize all matrices fields\n";
    int val;
    cin >> val;
    vector<int> matA = * new vector<int> (size, 0);
    vector<int> matB = * new vector<int> (size, 0);
    vector<int> res = * new vector<int> (size, 0);
    {
        initialization$(matA, matB, size, val);
        product$(matA, matB, res, size, m);
    }
    if (check(res, val, m)) { 
        cout << "Correct matmul\n";
    } 
    else { 
        cout << "Bad matmul\n";
    } 
}


