//check
bool check (int res[], int init_val, int size){
    int i;
    for(i = 0; i<size; i = i+1){
        int j;
        for(j = 0; j<size; j = j+1){
            if (init_val * init_val * size != res[i*size+j]){
                return false;
            }
        }
    }
    return true;
}


//initialisations of matrix matA and matB, size*size, with valors = val
int initialization(int matA [], int matB [], int size, int val){

    int i;
    parallel_for (i = 0; i<size ; i = i+1){
        matA[i] = val;
        matB[i] = val;
    }
    return 0;
}

//calcul of the product matrix matA * matrix matB, in res
//size = number of elements
//m = number of rows/columns (square matrix)
int product(int matA [], int matB [], int res[], int size, int m){
    int i;
    int j;
    int k;
    
    parallel_for (i = 0; i<m ; i = i+1){
    
            for (j = 0; j<m ; j = j+1){
        
                int tmp;
                tmp = 0;
            
                for (k = 0; k<m ; k = k+1){
                    tmp = tmp + matA[i*m+k] * matB[k*m+j];
                }
                not_sync{
                    res[i*m+j] = tmp;
                }
            }
        
    }
    return 0;
}

//main's start
write "Enter number of rows and columns \n(both matrices will be square and of the same size)\n";
int m;
read m;

int size;
size = m*m;

write "Enter the valor which will be used to initialize all matrices fields\n";
int val;
read val;

//matrix A, matrix B and matrix res = [A]*[B]
int matA [size];
int matB [size];
int res [size];

begin_parallel
{
    initialization(matA, matB, size, val);
    product(matA, matB, res, size, m);
}
end_parallel

if (check(res,val,m)){
    write "Correct matmul\n";
}
else{
    write "Bad matmul\n";
}
