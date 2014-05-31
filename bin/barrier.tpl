int k;
k = 10;

begin_parallel
private_var : k;
{
    k = k+3;
    barrier
    write k;
}
end_parallel
