# libgdx.polygon-generator
A tool for generating polygons and then creating colliders.

This can primarily be used to optimize collisions and is designed to work with Tiled (.tmx) maps.

Since each map in Tiled is created from tiles, and often there are a lot of such tiles, it is unprofitable to assign a collision to each tile. In addition, this can lead to problems with a collision out of the blue: https://www.iforce2d.net/b2dtut/ghost-vertices
