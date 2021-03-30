package com.sunlight.instruments;

import com.badlogic.gdx.math.Vector2;

public class Constants {
    /***
     A class with constants for working with other classes.
     @author aftern0on
     @version 1.00
     ***/

    /** Provisions that borders can accept **/
    public enum Position {
        TOP(new Vector2(0, 1), new Vector2(1, 1)),
        RIGHT(new Vector2(1, 0), new Vector2(1, 0)),
        DOWN(new Vector2(0, 0), new Vector2(0, 0)),
        LEFT(new Vector2(0, 0), new Vector2(0, 1));

        public Vector2 start, end;
        Position(Vector2 start, Vector2 end) {
            this.start = start;
            this.end = end;
        }

        /** Reverse current value **/
        public Position reverse() {
            if (this == TOP) return DOWN;
            if (this == DOWN) return TOP;
            if (this == LEFT) return RIGHT;
            if (this == RIGHT) return LEFT;
            return null;
        }
    }

    /** Parties and directions
     * Store the margins from the central position (x, y) for coordination in matrices **/
    public enum Direction {
        TOP_LEFT(new float[] {-1, -1}),
        TOP(new float[] {0, -1}),
        TOP_RIGHT(new float[] {1, -1}),

        CENTER_LEFT(new float[] {-1, 0}),
        CENTER_RIGHT(new float[] {1, 0}),

        DOWN_LEFT(new float[] {-1, 1}),
        DOWN(new float[] {0, 1}),
        DOWN_RIGHT(new float[] {1, 1});

        public float[] direction;
        Direction(float[] direction) { this.direction = direction; }

        /** Reverse current value **/
        public Direction reverse() {
            if (this == TOP_LEFT) return DOWN_RIGHT;
            if (this == TOP) return DOWN;
            if (this == TOP_RIGHT) return DOWN_LEFT;

            if (this == CENTER_LEFT) return CENTER_RIGHT;
            if (this == CENTER_RIGHT) return DOWN_LEFT;

            if (this == DOWN_LEFT) return TOP_RIGHT;
            if (this == DOWN) return TOP;
            if (this == DOWN_RIGHT) return TOP_LEFT;
            return null;
        }
    }
}