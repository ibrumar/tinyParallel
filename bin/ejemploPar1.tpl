
int localsum;
localsum = 1;
int sharedSum;
not_sync {
sharedSum = 2;
}
begin_parallel
private_var : localsum;
{
  sharedSum = sharedSum + localsum;
  localsum = localsum + 1;
  not_sync {
    sharedSum = sharedSum + localsum;
    not_sync {
    localsum = localsum + 1;
    }
  }
  sharedSum = sharedSum + localsum;
  localsum = localsum + 1;

  //begin_parallel
  //{
  
  //} end_parallel

} end_parallel

