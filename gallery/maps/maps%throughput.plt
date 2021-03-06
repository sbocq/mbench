set title "maps - 24*amd64, 1.7.0_04-ea-b06"
set xlabel "Elems"
set ylabel "Throughput[elems/s]"
set xtics (50000,250000,500000)
#set key out

#set term png
#set output "maps%throughput.png"
#set term postscript eps enhanced
#set output "maps%throughput.eps"

plot 'add_immutable-map.dat' using 1:4:($4*$3)/100 with errorlines title "add:immutable-map",\
'del_immutable-map.dat' using 1:4:($4*$3)/100 with errorlines title "del:immutable-map",\
'add_mutable-map.dat' using 1:4:($4*$3)/100 with errorlines title "add:mutable-map",\
'del_mutable-map.dat' using 1:4:($4*$3)/100 with errorlines title "del:mutable-map",\
'add_open-map.dat' using 1:4:($4*$3)/100 with errorlines title "add:open-map",\
'del_open-map.dat' using 1:4:($4*$3)/100 with errorlines title "del:open-map"
pause -1
