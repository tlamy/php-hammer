<?php

$dummy = function () {
    $x = 1;
    $y = 2;
    $z = 3;

    $a = compact('x');
    $b = [];
    $c = ['y'];

    ['x'=>$x] = [];

    return compact('y', 'z', 'x', 'a', 'b');
};