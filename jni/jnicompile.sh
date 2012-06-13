gcc  -o libfzformat.so -shared -Wl,-soname,libfzformat.so -I/usr/lib/jvm/java-6-openjdk/include/ -I/usr/lib/jvm/java-6-openjdk/include/linux/ fzformat.c -static -lc -lavformat

sudo cp libfzformat.so /usr/lib/
sudo ldconfig
