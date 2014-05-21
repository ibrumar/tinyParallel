int test (int i){
i = i+2;
if (false){
i = i+1;
barrier
i = 2;
}
return i;
}


int k;
k = 10;
int a;
int o;
int test;

begin_parallel
first_private_var : o; 
private_var : test,k, k,a;
{
k = k+3;
barrier
write k;
barrier
}
end_parallel
write k;

write "\n";
barrier
k = 44;
