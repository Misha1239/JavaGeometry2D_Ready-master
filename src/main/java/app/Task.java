package app;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.humbleui.jwm.MouseButton;
import io.github.humbleui.skija.*;
import misc.CoordinateSystem2d;
import misc.CoordinateSystem2i;
import misc.Vector2d;
import misc.Vector2i;
import panels.PanelLog;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static app.Colors.*;

/**
 * Класс задачи
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class Task {
    /**
     * Текст задачи
     */
    public static final String TASK_TEXT = """
            ПОСТАНОВКА ЗАДАЧИ:
            На плоскости задано множество точек. 
            Найти среди них такие две пары,
            что точка пересечения прямых,
            проведенных через эти пары точек,
            находится ближе всего к началу координат.""";


    /**
     *  коэффициент колёсика мыши
     */
    public Point des;
    private static final float WHEEL_SENSITIVE = 0.001f;

    /**
     * Вещественная система координат задачи
     */
    private final CoordinateSystem2d ownCS;
    /**
     * Список точек
     */
    private final ArrayList<Point> points;
    /**
     * Размер точки
     */
    private static final int POINT_SIZE = 3;
    /**
     * Последняя СК окна
     */
    private CoordinateSystem2i lastWindowCS;
    /**
     * Флаг, решена ли задача
     */
    private boolean solved;
    /**
     * Список точек в пересечении
     */
    private final ArrayList<Point> crossed;
    private ArrayList<Point> mp1= new ArrayList<Point>();
    private ArrayList<Point> mp2=new ArrayList<Point>();
    private ArrayList<Point> mp3=new ArrayList<Point>();
    private ArrayList<Point> mp4=new ArrayList<Point>();
    public Point point1;
    public Point point2;
    public Point point3;
    public Point point4;
    /**
     * Список точек в разности
     */
    private final ArrayList<Point> single;
    /**
     * Порядок разделителя сетки, т.е. раз в сколько отсечек
     * будет нарисована увеличенная
     */
    private static final int DELIMITER_ORDER = 10;

    /**
     * Задача
     *
     * @param ownCS  СК задачи
     * @param points массив точек
     */
    @JsonCreator
    public Task(@JsonProperty("ownCS") CoordinateSystem2d ownCS, @JsonProperty("points") ArrayList<Point> points) {
        this.ownCS = ownCS;
        this.points = points;
        this.crossed = new ArrayList<>();
        this.single = new ArrayList<>();
    }

    /**
     * Рисование
     *
     * @param canvas   область рисования
     * @param windowCS СК окна
     */
    public void paint(Canvas canvas, CoordinateSystem2i windowCS) {
        // Сохраняем последнюю СК
        lastWindowCS = windowCS;
        // рисуем координатную сетку
        renderGrid(canvas, lastWindowCS);
        // рисуем задачу
        renderTask(canvas, windowCS);
    }

    /**
     * Рисование задачи
     *
     * @param canvas   область рисования
     * @param windowCS СК окна
     */
    private void renderTask(Canvas canvas, CoordinateSystem2i windowCS) {
        canvas.save();
        // создаём перо
        try (var paint = new Paint()) {
            for (Point p : points) {
                paint.setColor(p.getColor());
                Vector2i windowPos = windowCS.getCoords(p.pos.x, p.pos.y, ownCS);
                // рисуем точку
                canvas.drawRect(Rect.makeXYWH(windowPos.x - POINT_SIZE, windowPos.y - POINT_SIZE, POINT_SIZE * 2, POINT_SIZE * 2), paint);
            }
            if (solved){
                // y-координату разворачиваем, потому что у СК окна ось y направлена вниз,
                // а в классическом представлении - вверх
                Vector2i windowPos = windowCS.getCoords(des.pos.x, des.pos.y, ownCS);
                // рисуем точку
                canvas.drawRect(Rect.makeXYWH(windowPos.x - POINT_SIZE, windowPos.y - POINT_SIZE, POINT_SIZE * 2, POINT_SIZE * 2), paint);
                paint.setColor(CROSSED_COLOR);
                paint.setStrokeWidth(5);
                // y-координату разворачиваем, потому что у СК окна ось y направлена вниз,
                // а в классическом представлении - вверх
                Vector2i windowPos2 = windowCS.getCoords(point1.getPos().x, point1.getPos().y, ownCS);
                Vector2i windowPos3 = windowCS.getCoords(point2.getPos().x, point2.getPos().y, ownCS);
                Vector2i windowPos4 = windowCS.getCoords(point3.getPos().x, point3.getPos().y, ownCS);
                Vector2i windowPos5 = windowCS.getCoords(point4.getPos().x, point4.getPos().y, ownCS);
                // рисуем точку
                canvas.drawRect(Rect.makeXYWH(windowPos.x - POINT_SIZE, windowPos.y - POINT_SIZE, POINT_SIZE * 2, POINT_SIZE * 2), paint);
                canvas.drawLine((float) windowPos2.x, (float) windowPos2.y, (float) windowPos3.x, (float) windowPos3.y, paint);
                canvas.drawLine((float) windowPos4.x, (float) windowPos4.y, (float) windowPos5.x, (float) windowPos5.y, paint);
            }
            }
            canvas.restore();
    }

    /**
     * Добавить точку
     *
     * @param pos      положение
     * @param pointSet множество
     */
    public void addPoint(Vector2d pos, Point.PointSet pointSet) {
        solved = false;
        Point newPoint = new Point(pos, pointSet);
        points.add(newPoint);
        PanelLog.info("точка " + newPoint + " добавлена в " + newPoint.getSetName());
    }


    /**
     * Клик мыши по пространству задачи
     *
     * @param pos         положение мыши
     * @param mouseButton кнопка мыши
     */
    public void click(Vector2i pos, MouseButton mouseButton) {
        if (lastWindowCS == null) return;
        // получаем положение на экране
        Vector2d taskPos = ownCS.getCoords(pos, lastWindowCS);
        // если левая кнопка мыши, добавляем в первое множество
        if (mouseButton.equals(MouseButton.PRIMARY)) {
            addPoint(taskPos, Point.PointSet.FIRST_SET);
            // если правая, то во второе
        }
    }


    /**
     * Добавить случайные точки
     *
     * @param cnt кол-во случайных точек
     */
    public void addRandomPoints(int cnt) {
        CoordinateSystem2i addGrid = new CoordinateSystem2i(30, 30);

        for (int i = 0; i < cnt; i++) {
            Vector2i gridPos = addGrid.getRandomCoords();
            Vector2d pos = ownCS.getCoords(gridPos, addGrid);
            // сработает примерно в половине случаев
            if (ThreadLocalRandom.current().nextBoolean())
                addPoint(pos, Point.PointSet.FIRST_SET);
        }
    }


    /**
     * Рисование сетки
     *
     * @param canvas   область рисования
     * @param windowCS СК окна
     */
    public void renderGrid(Canvas canvas, CoordinateSystem2i windowCS) {
        // сохраняем область рисования
        canvas.save();
        // получаем ширину штриха(т.е. по факту толщину линии)
        float strokeWidth = 0.03f / (float) ownCS.getSimilarity(windowCS).y + 0.5f;
        // создаём перо соответствующей толщины
        try (var paint = new Paint().setMode(PaintMode.STROKE).setStrokeWidth(strokeWidth).setColor(TASK_GRID_COLOR)) {
            // перебираем все целочисленные отсчёты нашей СК по оси X
            for (int i = (int) (ownCS.getMin().x); i <= (int) (ownCS.getMax().x); i++) {
                // находим положение этих штрихов на экране
                Vector2i windowPos = windowCS.getCoords(i, 0, ownCS);
                // каждый 10 штрих увеличенного размера
                float strokeHeight = i % DELIMITER_ORDER == 0 ? 5 : 2;
                // рисуем вертикальный штрих
                canvas.drawLine(windowPos.x, windowPos.y, windowPos.x, windowPos.y + strokeHeight, paint);
                canvas.drawLine(windowPos.x, windowPos.y, windowPos.x, windowPos.y - strokeHeight, paint);
            }
            // перебираем все целочисленные отсчёты нашей СК по оси Y
            for (int i = (int) (ownCS.getMin().y); i <= (int) (ownCS.getMax().y); i++) {
                // находим положение этих штрихов на экране
                Vector2i windowPos = windowCS.getCoords(0, i, ownCS);
                // каждый 10 штрих увеличенного размера
                float strokeHeight = i % 10 == 0 ? 5 : 2;
                // рисуем горизонтальный штрих
                canvas.drawLine(windowPos.x, windowPos.y, windowPos.x + strokeHeight, windowPos.y, paint);
                canvas.drawLine(windowPos.x, windowPos.y, windowPos.x - strokeHeight, windowPos.y, paint);
            }
        }
        // восстанавливаем область рисования
        canvas.restore();
    }


    /**
     * Очистить задачу
     */
    public void clear() {
        points.clear();
        solved = false;
        for (int i = 0; i < points.size(); i++) {
            for (int j = i + 1; j < points.size(); j++) {
                // сохраняем точки
                Point a = points.get(i);
                Point b = points.get(j);
                // если точки совпадают по положению
                if (a.pos.equals(b.pos) && !a.pointSet.equals(b.pointSet)) {
                    if (!crossed.contains(a)) {
                        crossed.add(a);
                        crossed.add(b);
                    }
                }
            }
        }

    }

    /**
     * Решить задачу
     */
    public void solve() {
        // очищаем списки
        crossed.clear();
        single.clear();
        mp1.clear();
        mp2.clear();
        mp3.clear();
        mp4.clear();
        // перебираем пары точек
        double xa,ya,xb,yb,k,a11=0;
        double xc,yc,xd,yd,k1,a1=0;
        int fl=0;
        double x=90,y=90;
        for (int i = 0; i < points.size(); i++) {
            for (int j = 0; j < points.size(); j++) {
                // сохраняем точки
                if (i != j) {
                    Point a = points.get(i);
                    xa = a.getPos().x;
                    ya = a.getPos().y;
                    Point b = points.get(j);
                    xb = b.getPos().x;
                    yb = b.getPos().y;
                    if (xa != xb) {
                        a11 = (ya - yb) / (xa - xb);
                    } else {
                        fl += 1;
                    }
                    k = ya - a11 * xa;
                } else {
                    continue;
                }
                for (int l = 0; l < points.size(); l++) {
                    for (int m = 0; m < points.size(); m++) {
                        if (m != l && (m != i && m != j || l != i && l != j)) {
                            Point c = points.get(m);
                            xc = c.getPos().x;
                            yc = c.getPos().y;
                            Point d = points.get(l);
                            xd = d.getPos().x;
                            yd = d.getPos().y;
                            if (xc != xd) {
                                a1 = (yc - yd) / (xc - xd);
                            } else {
                                fl += 2;
                            }
                            k1 = yc - a1 * xc;
                            if (fl == 0) {
                                if (a11 != a1) {
                                    x = (k1 - k) / (a11 - a1);
                                    y = a11 * x + k;
                                }
                            } else if (fl == 1) {
                                x = xa;
                                y = a1 * xa + k1;
                            } else if (fl == 2) {
                                x = xc;
                                y = a11 * xc + k;
                            }
                            if (fl < 3) {
                                Vector2d v = new Vector2d(x, y);
                                Point p=new Point(v, Point.PointSet.FIRST_SET);
                                crossed.add(p);
                                Vector2d v1 = new Vector2d(xa, ya);
                                Point p1=new Point(v1, Point.PointSet.FIRST_SET);
                                mp1.add(p1);
                                Vector2d v2 = new Vector2d(xb, yb);
                                Point p2=new Point(v2, Point.PointSet.FIRST_SET);
                                mp1.add(p2);
                                Vector2d v3 = new Vector2d(xc, yc);
                                Point p3=new Point(v3, Point.PointSet.FIRST_SET);
                                mp1.add(p3);
                                Vector2d v4 = new Vector2d(xd, yd);
                                Point p4=new Point(v4, Point.PointSet.FIRST_SET);
                                mp1.add(p4);
                            }
                        }
                    }
                }
            }
        }
        double xz,yz;
        double save= 100000;
        for (int i = 0; i < crossed.size(); i++) {
            Point z=crossed.get(i);
            xz = z.getPos().x;
            yz = z.getPos().y;
            if (Math.sqrt(xz*xz+yz*yz)<save){
                save=Math.sqrt(xz*xz+yz*yz);
            }
        }
        for (int i = 0; i < crossed.size(); i++) {
            Point z=crossed.get(i);
            xz = z.getPos().x;
            yz = z.getPos().y;
            if (Math.sqrt(xz*xz+yz*yz)==save){
                des=z;
                point1=mp1.get(i);
                point2=mp2.get(i);
                point3=mp3.get(i);
                point4=mp4.get(i);
                break;
            }
        }
        // задача решена
        solved = true;
    }

    /**
     * Получить тип мира
     *
     * @return тип мира
     */
    public CoordinateSystem2d getOwnCS() {
        return ownCS;
    }

    /**
     * Получить название мира
     *
     * @return название мира
     */
    public ArrayList<Point> getPoints() {
        return points;
    }

    /**
     * Получить список пересечений
     *
     * @return список пересечений
     */
    @JsonIgnore
    public ArrayList<Point> getCrossed() {
        return crossed;
    }

    /**
     * Получить список разности
     *
     * @return список разности
     */
    @JsonIgnore
    public ArrayList<Point> getSingle() {
        return single;
    }

    /**
     * Отмена решения задачи
     */
    public void cancel() {
        solved = false;
    }

    /**
     * проверка, решена ли задача
     *
     * @return флаг
     */
    public boolean isSolved() {
        return solved;
    }

    /**
     * Масштабирование области просмотра задачи
     *
     * @param delta  прокрутка колеса
     * @param center центр масштабирования
     */
    public void scale(float delta, Vector2i center) {
        if (lastWindowCS == null) return;
        // получаем координаты центра масштабирования в СК задачи
        Vector2d realCenter = ownCS.getCoords(center, lastWindowCS);
        // выполняем масштабирование
        ownCS.scale(1 + delta * WHEEL_SENSITIVE, realCenter);
    }

    /**
     * Получить положение курсора мыши в СК задачи
     *
     * @param x        координата X курсора
     * @param y        координата Y курсора
     * @param windowCS СК окна
     * @return вещественный вектор положения в СК задачи
     */
    @JsonIgnore
    public Vector2d getRealPos(int x, int y, CoordinateSystem2i windowCS) {
        return ownCS.getCoords(x, y, windowCS);
    }


    /**
     * Рисование курсора мыши
     *
     * @param canvas   область рисования
     * @param windowCS СК окна
     * @param font     шрифт
     * @param pos      положение курсора мыши
     */
    public void paintMouse(Canvas canvas, CoordinateSystem2i windowCS, Font font, Vector2i pos) {
        // создаём перо
        try (var paint = new Paint().setColor(TASK_GRID_COLOR)) {
            // сохраняем область рисования
            canvas.save();
            // рисуем перекрестие
            canvas.drawRect(Rect.makeXYWH(0, pos.y - 1, windowCS.getSize().x, 2), paint);
            canvas.drawRect(Rect.makeXYWH(pos.x - 1, 0, 2, windowCS.getSize().y), paint);
            // смещаемся немного для красивого вывода текста
            canvas.translate(pos.x + 3, pos.y - 5);
            // положение курсора в пространстве задачи
            Vector2d realPos = getRealPos(pos.x, pos.y, lastWindowCS);
            // выводим координаты
            canvas.drawString(realPos.toString(), 0, 0, font, paint);
            // восстанавливаем область рисования
            canvas.restore();
        }
    }

}
