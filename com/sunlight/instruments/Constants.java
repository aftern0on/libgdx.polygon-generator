package com.sunlight.instruments;

import com.badlogic.gdx.math.Vector2;

public class Constants {
    /***
     Класс с константами для работы с другими классами.

     @author aftern0on
     ***/

    // Положения, которые могут принимать границы
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
    }

    // Стороны и направления
    // Хранят отступы от центрального положения (x, y) для координации в матрицах
    public enum Directions {
        TOP_LEFT(new float[] {-1, -1}),
        TOP(new float[] {0, -1}),
        TOP_RIGHT(new float[] {1, -1}),

        CENTER_LEFT(new float[] {-1, 0}),
        CENTER_RIGHT(new float[] {1, 0}),

        DOWN_LEFT(new float[] {-1, 1}),
        DOWN(new float[] {0, 1}),
        DOWN_RIGHT(new float[] {1, 1});

        public float[] direction;
        Directions(float[] direction) { this.direction = direction; }
    }
}
