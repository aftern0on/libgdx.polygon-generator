package com.sunlight.instruments;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;

import com.sunlight.instruments.Constants.Directions;
import com.sunlight.instruments.Constants.Position;

public class Collision {
    /***
     Класс для реализации оптимизации коллизий.
     Объеденяет все коллизии тайлов друг с другом при возможности, минимизируя нагрузку на
     обработку каждой отдельной коллизии на локации.

     Для генерации сложных коллизий требуется указать ее фигуре все вершины.
     Данный класс содержит инструменты для получения всех границ с последующим получением
     вершин. Данный класс может:

     1. Получить все области регионов а также сгенерировать их полигоны.
     2. Получить все области и разбить их на как можно меньшее количество прямоугольных областей.

     @author aftern0on
     ***/

    // Таблица объектов-коллизий
    public static class CellList extends Group {
        private final Array<Polygon> polygons; // Найденые области
        private final Array<Region> regions; // Ряды тайлов
        private final Array<Cell.Bound> bounds; // Все найденные границы
        private final Cell[][] cells; // Таблица для взаимодействия с окружающими объектами
        private HashMap<Constants.Position, Position[]> turns; // Повороты для итерации границ

        // Создаем таблицу клеток, итерация матрицы, поиск объектов
        public CellList(TiledMapTileLayer layer) {
            cells = new Cell[layer.getHeight() + 3][layer.getWidth() + 3];
            bounds = new Array<>(); // Все возможные границы
            regions = new Array<>();
            polygons = new Array<>();
            buildTurnPriority();

            // Создание клеток
            for (int y = 1; y < layer.getHeight() + 1; y++) {
                for (int x = 1; x < layer.getWidth() + 1; x++) {
                    TiledMapTileLayer.Cell cell = layer.getCell(x - 1, y - 1);
                    // В этом условии можно осуществить фильтр для получения нужных коллизий
                    // По умолчанию - все объекты
                    if (cell != null) {
                        addCell(new Cell(this, x - 1, y - 1));
                    }
                }
            }

            // Обработка клеток, поиск "соседей", генерация "контуров"
            for (int y = 1; y < layer.getHeight() + 1; y++) {
                for (int x = 1; x < layer.getWidth() + 1; x++) {
                    if (cells[y][x] != null) {
                        cells[y][x].releaseAround(); // Клетка ищет и получает всех соседей
                        cells[y][x].releaseBounds(); // Клетка генерирует границы
                    }
                }
            }

            // Соеденение всех контуров между собой
            for (int y = 1; y < layer.getHeight() + 1; y++)
                for (int x = 1; x < layer.getWidth() + 1; x++)
                    if (cells[y][x] != null)
                        for (Cell.Bound bound : cells[y][x].bounds.values())
                            if (bound != null)
                                for (Cell.Bound overlap : bounds)
                                    bound.intersect(overlap);

            createPolygons();
            foundRectangleRegions();
        }

        // Создать новый регион
        private void addRegion(Cell start) {
            Region region = new Region(start, this);
            region.searchRow();
            addActor(region);
            regions.add(region);
        }

        // Поиск и объеденение всех регионов в более большие
        private void foundRectangleRegions() {
            // Создание регионов
            for (Cell[] row : cells) {
                for (Cell element : row) {
                    if (element != null) {
                        if (element.region == null) {
                            addRegion(element);
                        }
                    }
                }
            }

            // Итерация регионов сверху вниз, поиск дчерних регионов и их слияние
            regions.reverse();
            for (Region parent : regions) {
                // Проверка региона
                // Если регион не является дочерним то он может являться родительским
                if (parent.merged == null) {
                    // Если регион не является дочерним то он может являться родительским
                    parent.isParent = true;

                    // Поиск подходящих дочерних регионов для слияния
                    // Поиск будет продолжаться пока не найдутся все дочерние регионы
                    boolean founded; // Найден ли дочерний регион
                    do {
                        founded = false;
                        for (int index = 0; index < regions.size; index++) {
                            Region child = regions.get(index);
                            if (child != parent && child.merged == null) {
                                // Поиск подходящего дочернего региона
                                // Дочерний и родительский регионы должны быть одной ширины
                                // Также дочерний регион должен находиться строго под родительским
                                if ((parent.start.getX() == child.start.getX()) &&
                                        (parent.end.getX() == child.end.getX()) &&
                                        (parent.end.getY() - 1 == child.start.getY())) {
                                    founded = true;
                                    parent.mergeRegions(child);
                                    break; // Завершение поиска, начало нового
                                }
                            }
                        }
                    } while (founded);
                }
            }
        }

        // Указание приоритетов для правильного построения фигуры
        private void buildTurnPriority() {
            turns = new HashMap<>();

            turns.put(Position.TOP, new Position[]
                    {Position.LEFT, Position.DOWN, Position.TOP, Position.RIGHT});
            turns.put(Position.RIGHT, new Position[]
                    {Position.TOP, Position.LEFT, Position.RIGHT, Position.DOWN});
            turns.put(Position.DOWN, new Position[]
                    {Position.RIGHT, Position.TOP, Position.DOWN, Position.LEFT});
            turns.put(Position.LEFT, new Position[]
                    {Position.DOWN, Position.RIGHT, Position.LEFT, Position.TOP});
        }

        // Создание полигонов областей
        public void createPolygons() {
            // Прохождение по всем найденым областям
            for (Cell.Bound bound : bounds) {
                if (!bound.used) {
                    // Последовательное построение контура
                    Cell.Bound previous = null;
                    Cell.Bound current = bound;
                    Array<Float> temp = new Array<>();

                    // Генерация контура по построенному пути по приоритету
                    // Помещает сопуствующие координаты в список а затем генерирует полигон
                    do {
                        // Первый элемент будет отмечаться использованным в последнюю очередь
                        // Это делается для правильного завершения генерации полигона
                        current.used = previous != null;

                        // Проверка всех
                        for (Position turn : turns.get(current.position)) {
                            Cell.Bound next = current.overlaps.get(turn);
                            if (next != null) {
                                // Выбор правильного пути
                                if (!next.used) {
                                    // Проверка нахождения вершины на одной линии
                                    // В таком случае создавать вершину не нужно
                                    if (previous == null) {
                                        temp.add(current.end.x + getX());
                                        temp.add(current.end.y + getY());
                                    } else if (!(previous.end.x == next.end.x ||
                                            previous.end.y == next.end.y)) {
                                        temp.add(current.end.x + getX());
                                        temp.add(current.end.y + getY());
                                    }
                                    previous = current;
                                    current = next;
                                    break;
                                }
                            }
                        }
                    } while (!current.used);

                    // Конвентирование Array<Float> -> float[]
                    float[] vertices = new float[temp.size];
                    for (int index = 0; index < temp.size; index++) {
                        vertices[index] = temp.get(index);
                    }

                    // Создание полигона и добавление его в список
                    Polygon area = new Polygon();
                    area.setVertices(vertices);

                    polygons.add(area);
                }
            }
        }

        // Получение всех полигонов
        public Array<Polygon> getPolygons() {
            return polygons;
        }

        // Получение всех прямоугольных областей
        public Array<Region> getRegions() {
            Array<Region> parents = new Array<>();

            // Сбор всех регионов-родителей
            for (Region region : regions) if (region.isParent) parents.add(region);

            return parents;
        }

        // Получить клетку из матрицы по позиции
        private Cell getCell(int x, int y) {
            return cells[y + 1][x + 1];
        }

        // Добавляет новые клетки в матрицу объектов, коллизии которых нужно слить
        private void addCell(Cell cell) {
            cells[(int) cell.getY() + 1][(int) cell.getX() + 1] = cell;
            addActor(cell);
        }

        // Объект ряда клеток
        // Может являться родительским: содержит в себе всю площадь при объеденении.
        public static class Region extends Group {
            private boolean isParent; // Является ли регион родителским
            private Region merged; // Первый регион, с которого началось последующее слияние
            public Cell start, end; // Стартовая и конечная точки для определения пропорций региона
            public CellList list; // Родительская таблица

            public Region(Cell start, CellList list) {
                start.addToRegion(this);
                this.isParent = false;
                this.list = list;
                this.merged = null;
                this.start = start;
                this.end = start;
            }

            // Объеденение регионовов
            private void mergeRegions(Region child) {
                child.merged = this;
                this.isParent = true;
                this.end = child.end;
            }

            // Получение полигона региона
            public Polygon getPolygon() {
                // Определение вершин
                float[] vertices = new float[8];

                vertices[0] = start.getX() + list.getX(); vertices[1] = start.getY() + list.getY() + 1; // Верхняя-правая
                vertices[2] = end.getX() + list.getX() + 1; vertices[3] = start.getY() + list.getY() + 1; // Верхняя левая
                vertices[4] = end.getX() + list.getX() + 1; vertices[5] = end.getY() + list.getY(); // Нижняя-левая
                vertices[6] = start.getX() + list.getX(); vertices[7] = end.getY() + list.getY(); // Нижняя-правая

                // Возвращаемый полигон можно будет использовать для построения тела
                return new Polygon(vertices);
            }

            // Получение ряда блоков
            private void searchRow() {
                Cell right = end.around.get(Directions.CENTER_RIGHT);
                if (right != null) {
                    end = right;
                    right.addToRegion(this);
                    searchRow();
                }
            }
        }

        // Объект клетки, тайл, который нужно объеденить
        static class Cell extends Group {
            private Region region; // Ряд к которому принадлежит клетка
            private final HashMap<Directions, Cell> around; // Соседние клетки
            private final HashMap<Position, Bound> bounds; // Границы будущего коллайдера
            private final CellList list; // Матрица, к которой принадлежит клетка

            public Cell(CellList list, int x, int y) {
                this.list = list;
                this.region = null;
                this.around = new HashMap<>();
                this.bounds = new HashMap<>();

                setBounds(x, y, 1, 1);
            }

            // Добавление клетки в регион
            public void addToRegion(Region region) {
                this.region = region;
            }

            // Получение всех рядомстоящих объектов
            public void releaseAround() {
                for (Directions direction : Directions.values()) {
                    Cell cell = list.getCell(
                            (int) (getX() + direction.direction[0]),
                            (int) (getY() - direction.direction[1]));
                    around.put(direction, cell);
                }
            }

            // Генерация границ
            public void releaseBounds() {
                // Обнуление всех границ
                for (Position position : Position.values())
                    bounds.put(position, null);

                // Определение границ
                // Границ не может существовать в стороне, смежной с другой клеткой
                if (around.get(Directions.TOP) == null)
                    addBound(new Bound(Position.TOP, this));

                if (around.get(Directions.CENTER_RIGHT) == null)
                    addBound(new Bound(Position.RIGHT, this));

                if (around.get(Directions.DOWN) == null)
                    addBound(new Bound(Position.DOWN, this));

                if (around.get(Directions.CENTER_LEFT) == null)
                    addBound(new Bound(Position.LEFT, this));
            }

            // Добавить границу к клетке
            public void addBound(Bound bound) {
                addActor(bound);
                list.bounds.add(bound);
                bounds.put(bound.getPosition(), bound);
            }

            // Объект контура, по которому будет построена коллизия
            public static class Bound extends Actor {
                private final HashMap<Position, Bound> overlaps; // Пересекаемые границы
                private final Position position; // Позиция грани относительно клетки
                public final Rectangle rect; // Граница и конец границы
                public boolean used; // Используется для завершения цикла получения вершин
                public Vector2 end; // Окончание сегмента границы

                public Bound(Position position, Cell cell) {
                    this.overlaps = new HashMap<>();
                    this.position = position;
                    this.rect = new Rectangle();
                    this.end = new Vector2(
                            cell.getX() + position.end.x,
                            cell.getY() + position.end.y);

                    this.used = false;

                    // Позиция границы относительно клетки
                    setPosition(position.start.x, position.start.y);

                    // Размер границы
                    if (position == Position.TOP || position == Position.DOWN)
                        setSize(1.1f, 0.1f);
                    else
                        setSize(0.1f, 1.1f);
                    this.rect.set(
                            cell.getX() + getX(), cell.getY() + getY(),
                            getWidth(), getHeight());
                }

                // Получение позиции границы
                public Position getPosition() {
                    return position;
                }

                // Проверка пересечения конца текущей границы с данной
                public void intersect(Bound bound) {
                    if (Intersector.intersectSegmentRectangle(
                            end.x + 0.01f,
                            end.y + 0.01f,
                            end.x + 0.02f,
                            end.y + 0.02f, bound.rect) && !this.equals(bound)) {
                        overlaps.put(bound.position, bound);
                    }
                }
            }
        }
    }
}