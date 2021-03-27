# libgdx.polygon-generator
A tool for generating polygons and then creating colliders.

This can primarily be used to optimize collisions and is designed to work with Tiled (.tmx) maps.

Since each map in Tiled is created from tiles, and often there are a lot of such tiles, it is unprofitable to assign a collision to each tile. In addition, this can lead to problems with a collision out of the blue: https://www.iforce2d.net/b2dtut/ghost-vertices, it looks something like this: ![Original tilemap](https://github.com/aftern0on/libgdx.polygon-generator/blob/main/img/original.png)

To minimize the load on creating colliders for all these objects, you should reduce the number of rectangles to a minimum.
You can get an array of the minimum number of rectangular polygons by giving the class your collision layer, and then iterate them to create the body:
```java
// Getting regions
Array<CellList.Region> regions = new CellList(tiledMapLayer).getRegions();

// Create simple body
BodyDef bodyDef = new BodyDef();
bodyDef.type = BodyDef.BodyType.StaticBody;
Body body = world.createBody(bodyDef);

// Using region polygons to create body fixtures
for (Collision.CellList.Region region : regions) {
    PolygonShape shape = new PolygonShape();
    shape.set(region.getPolygon().getVertices());
    body.createFixture(shape, 0);
    shape.dispose();
}
```
As a result, you will transform all the tiles into a single body with multiple rectangular fixtures: ![Result with rect polygons](https://github.com/aftern0on/libgdx.polygon-generator/blob/main/img/merge_rects.png)

You can also get whole polygons of areas. However, if the shape is too complex and has more than 8 vertices, then it is not recommended to make a body out of it. This way you can get the perimeter of all the regions and their vertices respectively and do whatever you want with them:
```java
// Getting polygons of all areas
Array<Polygon> polygons = new CellList(tiledMapLayer).getPolygons();

// Iterating polygons
for (Polygon polygon : polygons) {
    // Getting the vertices of a region
    float[] vertex = polygon.getVertices();
    ...
}
```
Result of getting whole polygons:
![Result with whole polygons](https://github.com/aftern0on/libgdx.polygon-generator/blob/main/img/merge_all.png)

The table, cells, and borders that are used for calculations are extended from `Actor`, you can add `CellList` to the scene and debug them to see their borders.
