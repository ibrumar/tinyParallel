int k;
k = 10;
int a;

begin_parallel
first_private_var : k; 
private_var : a;
{
    k= k+77;
    write k;
    write " ";
}
end_parallel
