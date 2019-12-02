#!/bin/sh

mkdir -p build
pushd build

cmake ..
make
mv tictactoe-cpp ..
cp compile_commands.json ..

popd
