# libgdx.polygon-generator
A tool for generating polygons and then creating colliders.

[![](https://jitpack.io/v/aftern0on/libgdx.polygon-generator.svg)](https://jitpack.io/#aftern0on/libgdx.polygon-generator)

[1. What is it for?](https://github.com/aftern0on/libgdx.polygon-generator#what-is-it-for)

[2. Installation](https://github.com/aftern0on/libgdx.polygon-generator#installation)

[3. Features of the tool](https://github.com/aftern0on/libgdx.polygon-generator#features-of-the-tool)

[4. Filtering](https://github.com/aftern0on/libgdx.polygon-generator#filtering)

[5. Other](https://github.com/aftern0on/libgdx.polygon-generator#other)

# What is it for?
**This can primarily be used to optimize collisions and is designed to work with Tiled (.tmx) maps.**
Since each map in Tiled is created from tiles, and often there are a lot of such tiles, it is unprofitable to assign a collision to each tile. In addition, this can lead to problems with a collision out of the blue: https://www.iforce2d.net/b2dtut/ghost-vertices, it looks something like this: ![Original tilemap](https://github.com/aftern0on/libgdx.polygon-generator/blob/main/img/original.png)

# Installation

This tool can be downloaded via [jitpack](https://jitpack.io/#aftern0on/libgdx.polygon-generator).

Make sure that you have jitpack defined as repository.

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

Add the dependency. If you're using libGDX you need to add it to the core module.
```groovy
dependencies {
    implementation 'com.github.aftern0on:libgdx.polygon-generator:VERSION'
}
```

Check the version tag above or [click here](https://jitpack.io/#aftern0on/libgdx.polygon-generator) for the version.

# Features of the tool
To minimize the load on creating colliders for all these objects, you should reduce the number of rectangles to a minimum.
You can get an array of the minimum number of rectangular polygons by giving the class your collision layer, and then use regions to create the body:
```java
// Getting rect regions
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

# Filtering
Filtering will help you determine which objects should be taken into account when generating polygons. When iterating through all the objects of the matrix, the `filter()` function is executed, which decides whether to consider the object as part of the polygon. If the function returns `true`, the object is taken into account, if `false`, it is not. By default, the function always returns `true`. You can override this function to decide which objects should not be part of the polygon:
```java
// Override filter function
// For example, to reduce the territory
CellList list = new CellList(tiledMapLayer) {
    @Override
    public boolean filter(Cell cell, int x, int y) {
        // If the cell exists and meets the specified condition it returns true
        if (cell != null) return (x < 25 && y < 20);

        // If the object is empty it is ignored
        else return false;
    }
}
```
![Result with the use of territory restriction filtering](https://github.com/aftern0on/libgdx.polygon-generator/blob/main/img/filter_bounds.png)

You can use the data from `TileMapTile`, for example, to get the custom properties that you set in Tiled:
```java
// Override filter function
// Filtering elements by custom properties
CellList list = new CellList(tiledMapLayer) {
    @Override
    public boolean filter(Cell cell, int x, int y) {
        // Getting value of the "name" custom property
        // If this value is equal to "bookcase", this element will be filtered out
        if (cell != null)
            return (!cell.getTile().getProperties().get("name", String.class).equals("bookcase"));

        // If the object is empty it is ignored
        else return false;
    }
}
```
![Result with filtering by user properties](https://github.com/aftern0on/libgdx.polygon-generator/blob/main/img/filter_types.png)

You can also invert the result of the function to invert the territory and get its polygons. Note that you will not be able to get a `TileMapTile` object from empty cells:
```java
// Override filter function
// Inverting
CellList list = new CellList(tiledMapLayer) {
    @Override
    public boolean filter(Cell cell, int x, int y) {
        // Inverting result
        return !super.filter(cell, x, y);
    }
}
```
![Result with a inversible filter](https://github.com/aftern0on/libgdx.polygon-generator/blob/main/img/filter_inverse.png)
