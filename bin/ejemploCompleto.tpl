int errorManager(int code) {
	write "error: index out of bounds";
	exit(0);
	return 0;
}

write "Enter the size of the vectors a b for the dot product computation";
read N;

// a and b are vectors of size N
// all elements will be initialized to 0
int a[N-1]; //dinamic mem.
a[0] = 8;
int b[N-1];
b[0] = 5;
int P;
P = getNumProcessors();
int c[P];
int d[P];
c[0] = 1;
d[0] = 1;
int i;
for(i=0; i <= P-1; i = i + 1){
	c[i] = i;
	d[i] = i*2;
}
int pos;
pos = N - 4;
if (pos >= 0) {
	int e[N - pos - 1];
	int f[N - pos - 1];
}
else {
	errorManager(0);
}

begin_parallel
private_var : localsum;
{
	parallel_for(i=0; i <= N - 1; i = i + 1) {
		localsum = a[i]*b[i];
	}

	sum = sum + localsum;
	i = getThreadId();
	not_sync{
		c[i] = d[i];
	}
	if (pos >= 0) {
		e := f $N -pos - 1$;
	}
} end_parallel

