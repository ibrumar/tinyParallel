//test
int localsum;
localsum = 1;
int sharedSum;

not_sync {
sharedSum = 2;
}

int a[3];

begin_parallel
private_var : localsum;
{
  sharedSum = sharedSum + localsum;
  localsum = localsum + 1;
  not_sync {
    sharedSum = sharedSum + localsum;
    //not_sync {
    //localsum = localsum + 1;
    //}

    a[2] = a[2] + sharedSum;
  }
  a[2] = a[2] + sharedSum;
 

  sharedSum = sharedSum + localsum;
  localsum = localsum + 1;

//  begin_parallel
//  {
  
//  } end_parallel

} end_parallel

