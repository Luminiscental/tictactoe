#!/bin/sh

mkdir -p build
pushd build

cmake ..
make
mv tictactoe ..
cp compile_commands.json ..

popd
