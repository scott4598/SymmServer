#!/bin/bash
dir="/tmp/Syrs";
syrsvers="$(syrs -V)";
if test -r "${dir}/results${variant}.syrs-version"; then oldvers="$(< "${dir}/results${variant}.syrs-version")"; else oldvers="none"; fi;
if [ "$oldvers" != "$syrsvers" ]; then
   OCAMLRUNPARAM="h=3G" syrs -ll w -tf -exceptions,-taints,levels=all,heapdom=deepalias "${dir}/types.classes" -o "${dir}/types.classes_bin" -o "${dir}/types.class_stats" && OCAMLRUNPARAM="h=3G" syrs -ll w -tf -exceptions,-taints,levels=all,heapdom=deepalias "${dir}/types.classes_bin" "${dir}/Meth/all.secstubs" "${dir}/Meth/all.meth_files" -of secsum -rf -dr -rf cuddmem=4GiB -pf timeout=5m --methskip-cond 300 --safe-walk -o "${dir}/results${variant}.secsums" -o "${dir}/results${variant}.meth_stats" && echo "$syrsvers" > "${dir}/results${variant}.syrs-version"
fi
