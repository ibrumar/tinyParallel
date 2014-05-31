int k;
int cpt;
cpt = 0;
begin_parallel
private_var : k;
{
    parallel_for (k = 0; k<100; k = k + 1){
        cpt = cpt+1;
    }
} 
end_parallel
