package com.example.checkers4x;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import com.example.checkers4x.Triangle;
import android.util.Log;
import android.app.AlertDialog;
import android.app.Activity;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;

public class CheckersView extends View {
    private static final int BOARD_SIZE = 8;
    private int[][] board;
    private float cellSize;
    private Paint paint;
    private float baseRotation = 45;
    private float rotationAngle = 0;
    private Triangle[] triangles;
    private Bitmap whitePiece, blackPiece, whiteFlipped, blackFlipped;
    private int currentPlayer = 1; // 1: WR, 2: BR, 3: WF, 4: BF

    private boolean hungerEpoch = false;
    private List<Integer> wrPrison = new ArrayList<>();
    private List<Integer> brPrison = new ArrayList<>();
    private List<Integer> wfPrison = new ArrayList<>();
    private List<Integer> bfPrison = new ArrayList<>();
    private int selectedX = -1;
    private int selectedY = -1;
    private boolean isChainJumpInProgress = false;
    private List<Point> chainJumpMoves = null;
    private int currentMove = 1;
    private int currentCycle = 1;

    // Добавляем переменные для креста
    private Paint crossPaint;
    private float crossSize;
    private RectF crossTouchArea;

    // Добавляем переменные для кнопки
    private RectF buttonRect;
    private Paint buttonPaint;
    private float screenSize;
    private Paint buttonTextPaint;

    // Добавляем переменные для отслеживания выбывших игроков
    private boolean isPlayer1Eliminated = false; // WR (Белые справа)
    private boolean isPlayer2Eliminated = false; // BR (Черные справа)
    private boolean isPlayer3Eliminated = false; // WF (Белые спереди)
    private boolean isPlayer4Eliminated = false; // BF (Черные спереди)

    // Добавляем переменные для отслеживания оригинальной последовательности
    private int originalSequence = 1; // 1-2-3-4-1...

    // Добавляем флаг для отслеживания активности диалога
    private boolean isDialogShowing = false;

    // Добавляем в класс CheckersView:
       private RectF resetButtonRect;
    private Paint resetButtonPaint;
    private Point targetMove = null; // Выбранная цель для хода
    private List<List<Point>> possiblePaths = null; // Список всех возможных маршрутов к цели
    // Добавляем конструктор для совместимости с MainActivity
    public CheckersView(Context context) {
        this(context, null);
    }

    public CheckersView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        board = new int[BOARD_SIZE][BOARD_SIZE];
        initializeBoard();
        setBackgroundColor(Color.GRAY);

        // Load checker images
        whitePiece = BitmapFactory.decodeResource(getResources(), R.drawable.white);
        blackPiece = BitmapFactory.decodeResource(getResources(), R.drawable.black);
        whiteFlipped = BitmapFactory.decodeResource(getResources(), R.drawable.whitewithdot);
        blackFlipped = BitmapFactory.decodeResource(getResources(), R.drawable.blackwithdot);

        // Remove background from images
        whitePiece = removeBackgroundColor(whitePiece, Color.WHITE);
        blackPiece = removeBackgroundColor(blackPiece, Color.WHITE);
        whiteFlipped = removeBackgroundColor(whiteFlipped, Color.WHITE);
        blackFlipped = removeBackgroundColor(blackFlipped, Color.WHITE);

        // Инициализация настроек креста
        crossPaint = new Paint();
        crossPaint.setColor(Color.RED);
        crossPaint.setStyle(Paint.Style.STROKE);
        crossPaint.setStrokeWidth(8);
        crossPaint.setStrokeCap(Paint.Cap.ROUND);

        // Настройки для кнопки (цвет)
        buttonPaint = new Paint();
        buttonPaint.setColor(Color.GRAY);
        buttonPaint.setStyle(Paint.Style.FILL);

        // Настройки для кнопки (текст)
        buttonTextPaint = new Paint();
        buttonTextPaint.setColor(Color.WHITE);
        buttonTextPaint.setTextSize(screenSize * 0.04f);
        buttonTextPaint.setTextAlign(Paint.Align.CENTER);
        buttonTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Инициализация кнопки перезапуска
        resetButtonPaint = new Paint();
        resetButtonPaint.setColor(Color.GREEN);
        resetButtonPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        resetButtonPaint.setAntiAlias(true);
    }

    private Bitmap removeBackgroundColor(Bitmap source, int colorToRemove) {
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = new int[width * height];
        source.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] == colorToRemove) {
                pixels[i] = Color.TRANSPARENT;
            }
        }

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }

    private void initializeBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = 0;
            }
        }

        // White Regular (WR) - внизу
        board[7][7] = 1; // H1
        board[6][7] = 1; // G1
        board[5][7] = 1; // F1
        board[7][6] = 1; // H2
        board[6][6] = 1; // G2
        board[7][5] = 1; // H3
        board[6][5] = 1;
        board[5][6] = 1;
        board[5][5] = 1;

        // Black Regular (BR) - справа
        board[0][7] = 2; // A1
        board[1][7] = 2; // B1
        board[2][7] = 2; // C1
        board[0][6] = 2; // A2
        board[1][6] = 2; // B2
        board[0][5] = 2; //
        board[1][5] = 2;
        board[2][6] = 2;
        board[2][5] = 2;

        // White Flipped (WF) - вверху
        board[0][0] = 3; // A8
        board[1][0] = 3; // B8
        board[2][0] = 3; // C8
        board[0][1] = 3; // A7
        board[1][1] = 3; // B7
        board[0][2] = 3; // A6
        board[1][2] = 3;
        board[2][1] = 3;
        board[2][2] = 3;

        // Black Flipped (BF) - слева
        board[7][0] = 4; // H8
        board[6][0] = 4; // G8
        board[5][0] = 4; // F8
        board[7][1] = 4; // H7
        board[6][1] = 4; // G7
        board[7][2] = 4; // H6
        board[6][2] = 4;
        board[5][1] = 4;
        board[5][2] = 4;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int screenSize = Math.min(getWidth(), getHeight());
        int boardSize = (int) (screenSize / Math.sqrt(2));
        cellSize = boardSize / BOARD_SIZE;

        canvas.save();

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        // Рисуем доску с поворотом
        canvas.translate(centerX, centerY);
        canvas.rotate(baseRotation + rotationAngle);
        canvas.translate(-boardSize / 2f, -boardSize / 2f);

        // Увеличиваем толщину линий
        paint.setStrokeWidth(6);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        // Настройка текста
        Paint textPaint = new Paint();
        textPaint.setTextSize(boardSize/16f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Сохраняем текущий угол поворота для компенсации при отрисовке букв
        float currentRotation = baseRotation + rotationAngle;

        // У клетки board[7][7] (нижний правый угол)
        // Правая часть (красный)
        paint.setColor(Color.BLACK); // Все линии черные
        paint.setStrokeWidth(6); // Исходная толщина линий
        paint.setStyle(Paint.Style.STROKE); // Только контуры
        Path bottomRightTriangle = new Path();
        bottomRightTriangle.moveTo(boardSize, boardSize/2f);
        bottomRightTriangle.lineTo(boardSize, boardSize);
        bottomRightTriangle.lineTo(boardSize + boardSize/2f, boardSize/2f);
        bottomRightTriangle.close();
        canvas.drawPath(bottomRightTriangle, paint);
        canvas.save();
        canvas.rotate(-currentRotation, boardSize + boardSize/8f, boardSize*3/4f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("А1", boardSize + boardSize/8f, boardSize*3/4f, textPaint);
        canvas.restore();

        // Левая часть (желтый)
        paint.setColor(Color.BLACK); // Все линии черные
        paint.setStrokeWidth(6); // Исходная толщина линий
        paint.setStyle(Paint.Style.STROKE); // Только контуры
        Path bottomRightLeftTriangle = new Path();
        bottomRightLeftTriangle.moveTo(boardSize, 0);
        bottomRightLeftTriangle.lineTo(boardSize, boardSize/2f);
        bottomRightLeftTriangle.lineTo(boardSize + boardSize/2f, boardSize/2f);
        bottomRightLeftTriangle.close();
        canvas.drawPath(bottomRightLeftTriangle, paint);
        canvas.save();
        canvas.rotate(-currentRotation, boardSize + boardSize/8f, boardSize/4f);
        textPaint.setColor(Color.BLACK);
        canvas.drawText("З2", boardSize + boardSize/8f, boardSize/4f, textPaint);
        canvas.restore();

        // У клетки board[0][7] (верхний правый угол)
        // Верхняя часть (красный)
        paint.setColor(Color.BLACK); // Все линии черные
        paint.setStrokeWidth(6); // Исходная толщина линий
        paint.setStyle(Paint.Style.STROKE); // Только контуры
        Path topRightTriangle = new Path();
        topRightTriangle.moveTo(boardSize/2f, 0);
        topRightTriangle.lineTo(boardSize, 0);
        topRightTriangle.lineTo(boardSize/2f, -boardSize/2f);
        topRightTriangle.close();
        canvas.drawPath(topRightTriangle, paint);
        canvas.save();
        canvas.rotate(-currentRotation, boardSize*3/4f, -boardSize/8f);
        textPaint.setColor(Color.BLACK);
        canvas.drawText("А2", boardSize*3/4f, -boardSize/8f, textPaint);
        canvas.restore();

        // Нижняя часть (желтый)
        paint.setColor(Color.BLACK); // Все линии черные
        paint.setStrokeWidth(6); // Исходная толщина линий
        paint.setStyle(Paint.Style.STROKE); // Только контуры
        Path topRightBottomTriangle = new Path();
        topRightBottomTriangle.moveTo(0, 0);
        topRightBottomTriangle.lineTo(boardSize/2f, 0);
        topRightBottomTriangle.lineTo(boardSize/2f, -boardSize/2f);
        topRightBottomTriangle.close();
        canvas.drawPath(topRightBottomTriangle, paint);
        canvas.save();
        canvas.rotate(-currentRotation, boardSize/4f, -boardSize/8f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("З3", boardSize/4f, -boardSize/8f, textPaint);
        canvas.restore();

        // У клетки board[0][0] (верхний левый угол)
        // Правая часть (желтый)
        paint.setColor(Color.BLACK); // Все линии черные
        paint.setStrokeWidth(6); // Исходная толщина линий
        paint.setStyle(Paint.Style.STROKE); // Только контуры
        Path topLeftRightTriangle = new Path();
        topLeftRightTriangle.moveTo(0, boardSize/2f);
        topLeftRightTriangle.lineTo(0, boardSize);
        topLeftRightTriangle.lineTo(-boardSize/2f, boardSize/2f);
        topLeftRightTriangle.close();
        canvas.drawPath(topLeftRightTriangle, paint);
        canvas.save();
        canvas.rotate(-currentRotation, -boardSize/8f, boardSize*3/4f);
        textPaint.setColor(Color.BLACK);
        canvas.drawText("З4", -boardSize/8f, boardSize*3/4f, textPaint);
        canvas.restore();

        // Левая часть (красный)
        paint.setColor(Color.BLACK); // Все линии черные
        paint.setStrokeWidth(6); // Исходная толщина линий
        paint.setStyle(Paint.Style.STROKE); // Только контуры
        Path topLeftTriangle = new Path();
        topLeftTriangle.moveTo(0, 0);
        topLeftTriangle.lineTo(0, boardSize/2f);
        topLeftTriangle.lineTo(-boardSize/2f, boardSize/2f);
        topLeftTriangle.close();
        canvas.drawPath(topLeftTriangle, paint);
        canvas.save();
        canvas.rotate(-currentRotation, -boardSize/8f, boardSize/4f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("А3", -boardSize/8f, boardSize/4f, textPaint);
        canvas.restore();

        // У клетки board[7][0] (нижний левый угол)
        // Верхняя часть (желтый)
        paint.setColor(Color.BLACK); // Все линии черные
        paint.setStrokeWidth(6); // Исходная толщина линий
        paint.setStyle(Paint.Style.STROKE); // Только контуры
        Path bottomLeftTopTriangle = new Path();
        bottomLeftTopTriangle.moveTo(boardSize/2f, boardSize);
        bottomLeftTopTriangle.lineTo(boardSize, boardSize);
        bottomLeftTopTriangle.lineTo(boardSize/2f, boardSize + boardSize/2f);
        bottomLeftTopTriangle.close();
        canvas.drawPath(bottomLeftTopTriangle, paint);
        canvas.save();
        canvas.rotate(-currentRotation, boardSize*3/4f, boardSize + boardSize/8f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("З1", boardSize*3/4f, boardSize + boardSize/8f, textPaint);
        canvas.restore();

        // Нижняя часть (красный)
        paint.setColor(Color.BLACK); // Все линии черные
        paint.setStrokeWidth(6); // Исходная толщина линий
        paint.setStyle(Paint.Style.STROKE); // Только контуры
        Path bottomLeftTriangle = new Path();
        bottomLeftTriangle.moveTo(0, boardSize);
        bottomLeftTriangle.lineTo(boardSize/2f, boardSize);
        bottomLeftTriangle.lineTo(boardSize/2f, boardSize + boardSize/2f);
        bottomLeftTriangle.close();
        canvas.drawPath(bottomLeftTriangle, paint);
        canvas.save();
        canvas.rotate(-currentRotation, boardSize/4f, boardSize + boardSize/8f);
        textPaint.setColor(Color.BLACK);
        canvas.drawText("А4", boardSize/4f, boardSize + boardSize/8f, textPaint);
        canvas.restore();

        // Возвращаем настройки кисти для рисования доски
        paint.setStyle(Paint.Style.FILL);

        drawBoard(canvas);

        // Создаем отдельный Paint для контура угловых клеток
        Paint cornerStrokePaint = new Paint();
        cornerStrokePaint.setStyle(Paint.Style.STROKE);
        cornerStrokePaint.setColor(Color.RED);
        cornerStrokePaint.setStrokeWidth(4f);

        // Верхний левый угол
        canvas.drawRect(2, 2, cellSize - 2, cellSize - 2, cornerStrokePaint);
        // Верхний правый угол
        canvas.drawRect((BOARD_SIZE-1)*cellSize + 2, 2, 
                       BOARD_SIZE*cellSize - 2, cellSize - 2, cornerStrokePaint);
        // Нижний левый угол
        canvas.drawRect(2, (BOARD_SIZE-1)*cellSize + 2, 
                       cellSize - 2, BOARD_SIZE*cellSize - 2, cornerStrokePaint);
        // Нижний правый угол
        canvas.drawRect((BOARD_SIZE-1)*cellSize + 2, (BOARD_SIZE-1)*cellSize + 2,
                       BOARD_SIZE*cellSize - 2, BOARD_SIZE*cellSize - 2, cornerStrokePaint);

        // Подсвечиваем выбранную клетку и доступные ходы
        if (selectedX != -1 && selectedY != -1) {
            paint.setColor(Color.GREEN);
            paint.setAlpha(128);
            canvas.drawRect(selectedX * cellSize, selectedY * cellSize,
                    (selectedX + 1) * cellSize, (selectedY + 1) * cellSize, paint);

            if (targetMove == null) {
                // Подсветка доступных ходов
                List<Point> availableMoves = getAvailableMoves(selectedX, selectedY);
                for (Point move : availableMoves) {
                    if (move.isEnemyJump) {
                        paint.setColor(Color.rgb(255, 165, 0)); // Оранжевый
                    } else {
                        paint.setColor(Color.GREEN);
                    }
                    paint.setAlpha(128);
                    canvas.drawRect(move.x * cellSize, move.y * cellSize,
                            (move.x + 1) * cellSize, (move.y + 1) * cellSize, paint);
                }
            } else {
                // Подсветка маршрутов к выбранной цели
                for (List<Point> path : possiblePaths) {
                    for (Point point : path) {
                        if (point.equals(targetMove)) {
                            paint.setColor(Color.GREEN); // Цель
                        } else if (board[point.y][point.x] != 0) {
                            // Подсветка шашек на пути
                            if (isAllyPiece(board[point.y][point.x])) {
                                paint.setColor(Color.GREEN); // Союзные шашки
                            } else {
                                paint.setColor(Color.rgb(255, 165, 0)); // Вражеские шашки
                            }
                        } else {
                            paint.setColor(Color.GRAY); // Промежуточные клетки
                        }
                        paint.setAlpha(128);
                        canvas.drawRect(point.x * cellSize, point.y * cellSize,
                                (point.x + 1) * cellSize, (point.y + 1) * cellSize, paint);
                    }
                }
            }
        }

        drawPieces(canvas, boardSize);

        // Восстанавливаем состояние канваса
        canvas.restore();

        // Отрисовка неподвижных элементов интерфейса

        // Настройка иконок
        float iconSize = cellSize * 1.5f; // Размер иконок в 1,5 раза больше клетки
        if (iconSize <= 0) return;

        // Счет
        Paint scorePaint = new Paint();
        scorePaint.setTextSize(screenSize * 0.16f);
        scorePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        scorePaint.setTextAlign(Paint.Align.CENTER);

        // Позиционирование
        float textBaseY = screenSize * 0.2f; // Текущая позиция текста
        float iconVerticalCenter = textBaseY - scorePaint.getTextSize()/2.8f; // Центр иконок

        // Масштабирование битмапов
        Bitmap scaledWhite = Bitmap.createScaledBitmap(whitePiece, (int)iconSize, (int)iconSize, true);
        Bitmap scaledWhiteFlipped = Bitmap.createScaledBitmap(whiteFlipped, (int)iconSize, (int)iconSize, true);
        Bitmap scaledBlack = Bitmap.createScaledBitmap(blackPiece, (int)iconSize, (int)iconSize, true);
        Bitmap scaledBlackFlipped = Bitmap.createScaledBitmap(blackFlipped, (int)iconSize, (int)iconSize, true);

        // Отображение белых элементов
        scorePaint.setColor(Color.WHITE);
        canvas.drawText(String.valueOf(getWhiteScore()), screenSize * 0.4f, textBaseY, scorePaint);
        canvas.drawBitmap(
            scaledWhite, 
            screenSize * 0.27f - iconSize*1.2f, // Слева от текста
            iconVerticalCenter - iconSize/2, 
            null
        );
        canvas.drawBitmap(
            scaledWhiteFlipped, 
            screenSize * 0.32f - iconSize*1.2f,
            iconVerticalCenter - iconSize/2,
            null
        );

        // Отображение черных элементов
        scorePaint.setColor(Color.BLACK);
        canvas.drawText(String.valueOf(getBlackScore()), screenSize * 0.6f, textBaseY, scorePaint);
        canvas.drawBitmap(
            scaledBlack, 
            screenSize * 0.55f + iconSize*1.2f,
            iconVerticalCenter - iconSize/2, 
            null
        );
        canvas.drawBitmap(
            scaledBlackFlipped, 
            screenSize * 0.6f + iconSize*1.2f, // Справа от текста
            iconVerticalCenter - iconSize/2, 
            null
        );
        // Добавление текста "ХОД X" и "ЦИКЛ Y"
        textPaint.setTextSize(screenSize * 0.08f); // Половина размера шрифта счета
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        // Условие для цвета текста
        if (currentCycle % 4 == 0) {
            textPaint.setColor(Color.RED); // Красный цвет каждые 4 цикла
        } else {
            textPaint.setColor(Color.WHITE); // Стандартный цвет
        }

        // Позиции текста
        float moveTextX = screenSize * 0.23f;
        float cycleTextX = screenSize * 0.8f;
        float textY = screenSize * 0.4f; // Над доской

        // Отрисовка текста Ход, Цикл
        canvas.drawText("ХОД " + currentMove, moveTextX, textY, textPaint);
        canvas.drawText("ЦИКЛ " + currentCycle, cycleTextX, textY, textPaint);

        // Отрисовка креста в правом верхнем углу
        crossSize = screenSize * 0.05f; // 5% от размера экрана
        float padding = screenSize * 0.02f; // отступ от края
        float startX = getWidth() - crossSize - padding;
        float startY = padding;
        
        // Область для определения касания
        crossTouchArea = new RectF(
            startX - padding,
            startY - padding,
            startX + crossSize + padding,
            startY + crossSize + padding
        );

        // Рисуем крест
        canvas.drawLine(
            startX,
            startY,
            startX + crossSize,
            startY + crossSize,
            crossPaint
        );
        canvas.drawLine(
            startX + crossSize,
            startY,
            startX,
            startY + crossSize,
            crossPaint
        );

        // Отрисовка кнопки под доской
        float buttonWidth = screenSize * 0.1f;
        float buttonHeight = screenSize * 0.08f;
        float buttonX = (getWidth() - buttonWidth) / 2;
        float buttonY = screenSize + cellSize*8f; // Под доской

        buttonRect = new RectF(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight);

        canvas.drawRoundRect(buttonRect, 20, 20, buttonPaint);
        canvas.drawText("Оставить центральные", 
            buttonRect.centerX(), 
            buttonRect.centerY() + buttonTextPaint.getTextSize()/3, 
            buttonTextPaint);

        // Настраиваем позицию стрелки
        float resetSize = crossSize;
        float resetX = startX;
        float resetY = startY; // Начинаем от верха креста
        
        // Смещаем стрелку вниз на высоту креста
        resetY += crossSize*1.5f;
        
        resetButtonRect = new RectF(resetX, resetY, resetX + resetSize, resetY + resetSize);
        
        // Рисуем стрелку со смещением
        float visualOffset = dpToPx(1); // Конвертируем 5pt в пиксели
        Path arrowPath = new Path();
        float width = resetButtonRect.width();
        float height = resetButtonRect.height();
        
        // Основание стрелки (правая сторона)
        arrowPath.moveTo(resetButtonRect.right, resetButtonRect.top + height/50f);
        
        // Наклонная линия к острию
        arrowPath.lineTo(resetButtonRect.left + width*0.01f, resetButtonRect.centerY());
        
        // Основание стрелки (правая сторона)
        arrowPath.lineTo(resetButtonRect.right, resetButtonRect.bottom - height/50f);
        
        // Замыкаем треугольник
        arrowPath.lineTo(resetButtonRect.right - width*0.2f, resetButtonRect.centerY());
        arrowPath.close();

        // Стиль отрисовки
        resetButtonPaint.setStyle(Paint.Style.FILL);
        resetButtonPaint.setColor(Color.GREEN);
        canvas.drawPath(arrowPath, resetButtonPaint);
    }

    private float dpToPx(int dp) {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 
            dp, 
            getResources().getDisplayMetrics()
        );
    }

    private void drawBoard(Canvas canvas) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                paint.setColor((i + j) % 2 == 0 ? Color.LTGRAY : Color.DKGRAY);
                canvas.drawRect(j * cellSize, i * cellSize, (j + 1) * cellSize, (i + 1) * cellSize, paint);
            }
        }
    }

    private void drawPieces(Canvas canvas, int boardSize) {
        float pieceSize = cellSize * 0.8f; // 80% of cell size
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                Bitmap piece = null;
                if (board[i][j] == 1) piece = whitePiece;
                else if (board[i][j] == 2) piece = blackPiece;
                else if (board[i][j] == 3) piece = whiteFlipped;
                else if (board[i][j] == 4) piece = blackFlipped;

                if (piece != null) {
                    // Resize the piece to fit the cell
                    Bitmap scaledPiece = getResizedBitmap(piece, (int) pieceSize, (int) pieceSize);
                    
                    // Calculate the position to center the piece in the cell
                    float x = j * cellSize + (cellSize - pieceSize) / 2f;
                    float y = i * cellSize + (cellSize - pieceSize) / 2f;
                    
                    canvas.drawBitmap(scaledPiece, x, y, null);
                }
            }
        }
    }

    private float[] transformTouchPoint(float x, float y) {
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        
        // Смещение к центру
        x -= centerX;
        y -= centerY;
        
        // Поворот на угол, обратный сумме базового угла и угла текущего игрока
        double angle = Math.toRadians(-(baseRotation + rotationAngle));
        float rotatedX = (float) (x * Math.cos(angle) - y * Math.sin(angle));
        float rotatedY = (float) (x * Math.sin(angle) + y * Math.cos(angle));
        
        // Смещение к началу доски
        int boardSize = (int) (Math.min(getWidth(), getHeight()) / Math.sqrt(2));
        rotatedX += boardSize / 2f;
        rotatedY += boardSize / 2f;
        
        return new float[]{rotatedX, rotatedY};
    }

    private Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            // Проверяем, было ли касание в области креста
            if (crossTouchArea != null && crossTouchArea.contains(x, y)) {
                Context context = getContext();
                if (context instanceof Activity) {
                    ((Activity) context).finish();
                }
                return true;
            }

            // Проверяем нажатие на кнопку
            if (buttonRect != null && buttonRect.contains(x, y)) {
                leaveOnlyCentralPieces();
                return true;
            }

            if (resetButtonRect.contains(x, y)) {
                resetGame();
                return true;
            }

            float[] touchPoint = transformTouchPoint(x, y); // Переименовал point на touchPoint
            int xPos = (int) (touchPoint[0] / cellSize);
            int yPos = (int) (touchPoint[1] / cellSize);

            if (isWithinBounds(xPos, yPos)) {
                if (selectedX == -1 && selectedY == -1) {
                    // Первое касание - выбор шашки
                    if (board[yPos][xPos] == currentPlayer) {
                        selectedX = xPos;
                        selectedY = yPos;
                        targetMove = null;
                        possiblePaths = null;
                        invalidate();
                    }
                } else if (targetMove == null) {
                    // Второе касание - выбор цели
                    List<Point> availableMoves = getAvailableMoves(selectedX, selectedY);
                    Point selectedMove = null;
                    for (Point move : availableMoves) {
                        if (move.x == xPos && move.y == yPos) {
                            selectedMove = move;
                            break;
                        }
                    }
                    if (selectedMove != null) {
                        targetMove = selectedMove;
                        possiblePaths = findAllPaths(selectedX, selectedY, xPos, yPos);
                        invalidate();
                    } else {
                        selectedX = -1;
                        selectedY = -1;
                        targetMove = null;
                        possiblePaths = null;
                        invalidate();
                    }
                } else {
                    // Третье касание - выбор маршрута
                    List<Point> selectedPath = null;
                    for (List<Point> path : possiblePaths) {
                        for (Point pathPoint : path) { // Переименовал point на pathPoint
                            if (pathPoint.x == xPos && pathPoint.y == yPos) {
                                selectedPath = path;
                                break;
                            }
                        }
                        if (selectedPath != null) break;
                    }
                    if (selectedPath != null) {
                        executePath(selectedPath);
                        selectedX = -1;
                        selectedY = -1;
                        targetMove = null;
                        possiblePaths = null;
                        switchPlayer();
                        rotateBoard();
                        updateMove();
                        invalidate();
                    } else {
                        selectedX = -1;
                        selectedY = -1;
                        targetMove = null;
                        possiblePaths = null;
                        invalidate();
                    }
                }
            }
        }
        return true;
    }

    private boolean isValidMove(int fromX, int fromY, int toX, int toY) {
        if (!isWithinBounds(fromX, fromY) || !isWithinBounds(toX, toY)) {
            return false;
        }

        // Проверяем, что начальная позиция содержит шашку текущего игрока
        if (board[fromY][fromX] != currentPlayer) {
            return false;
        }

        // Проверяем, что конечная позиция пуста
        if (board[toY][toX] != 0) {
            return false;
        }

        // Проверяем, что ход выполняется на соседнюю клетку
        int dx = Math.abs(toX - fromX);
        int dy = Math.abs(toY - fromY);
        return dx <= 1 && dy <= 1;
    }

    private void makeMove(int fromX, int fromY, int toX, int toY, Point movePoint) {
        if (movePoint != null && movePoint.isEnemyJump) {
            // Удаляем все шашки, через которые прыгали
            for (Point jumpedPiece : movePoint.jumpedPieces) {
                board[jumpedPiece.y][jumpedPiece.x] = 0;
            }
        }
        // Перемещаем шашку
        board[toY][toX] = board[fromY][fromX];
        board[fromY][fromX] = 0;

        // После выполнения хода
        checkPlayerElimination();
        
        // Проверяем условие победы только если выбыли все игроки одного цвета
        if ((isPlayer1Eliminated && isPlayer3Eliminated) || 
            (isPlayer2Eliminated && isPlayer4Eliminated)) {
            checkWinCondition();
        }
    }

    private boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < BOARD_SIZE && y >= 0 && y < BOARD_SIZE;
    }

    private void rotateBoard() {
        // Вращение доски так, чтобы столица следующего игрока была внизу
        switch (currentPlayer) {
            case 1: // WR
                rotationAngle = 0f;
                break;
            case 2: // BR
                rotationAngle = 90f;
                break;
            case 3: // WF
                rotationAngle = 180f;
                break;
            case 4: // BF
                rotationAngle = 270f;
                break;
        }
    }

    private void switchPlayer() {
        checkPlayerElimination();
        
        // Проверяем условие победы
        if ((isPlayer1Eliminated && isPlayer3Eliminated) || 
            (isPlayer2Eliminated && isPlayer4Eliminated)) {
            checkWinCondition();
            return;
        }

        // Переключаем оригинальную последовательность
        originalSequence = (originalSequence % 4) + 1;
        
        // Определяем фактического игрока
        currentPlayer = getActualPlayer(originalSequence);
    }

    // Метод для определения фактического игрока
    private int getActualPlayer(int sequencePlayer) {
        // Если игрок в последовательности активен - возвращаем его
        if (!isPlayerEliminated(sequencePlayer)) {
            return sequencePlayer;
        }
        
        // Если игрок выбыл - возвращаем союзника
        switch (sequencePlayer) {
            case 1: return 3; // WR -> WF
            case 2: return 4; // BR -> BF
            case 3: return 1; // WF -> WR
            case 4: return 2; // BF -> BR
            default: return sequencePlayer;
        }
    }

    // Метод проверки выбывания игрока
    private boolean isPlayerEliminated(int player) {
        switch (player) {
            case 1: return isPlayer1Eliminated;
            case 2: return isPlayer2Eliminated;
            case 3: return isPlayer3Eliminated;
            case 4: return isPlayer4Eliminated;
            default: return false;
        }
    }

    private List<Point> getAvailableMoves(int x, int y) {
        List<Point> moves = new ArrayList<>();
        boolean[][] visited = new boolean[BOARD_SIZE][BOARD_SIZE];
        visited[y][x] = true;

        // Добавляем все возможные прыжки через союзные шашки (существующая логика)
        findJumps(x, y, moves, visited);

        // Добавляем все возможные прыжки через вражеские шашки
        List<Point> enemyJumps = new ArrayList<>();
        findEnemyJumps(x, y, enemyJumps, new boolean[BOARD_SIZE][BOARD_SIZE]);

        // Добавляем обычные ходы
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int newX = x + dx;
                int newY = y + dy;
                if (isWithinBounds(newX, newY) && board[newY][newX] == 0) {
                    moves.add(new Point(newX, newY));
                }
            }
        }

        // Добавляем вражеские прыжки с пометкой
        for (Point jump : enemyJumps) {
            jump.isEnemyJump = true;
            moves.add(jump);
        }

        return moves;
    }
    private List<List<Point>> findAllPaths(int startX, int startY, int endX, int endY) {
        List<List<Point>> allPaths = new ArrayList<>();
        List<Point> availableMoves = getAvailableMoves(startX, startY);
        for (Point move : availableMoves) {
            if (move.x == endX && move.y == endY) {
                List<Point> path = new ArrayList<>();
                path.add(new Point(startX, startY));
                if (move.isEnemyJump || Math.abs(move.x - startX) > 1 || Math.abs(move.y - startY) > 1) {
                    for (Point jumped : move.jumpedPieces) {
                        path.add(new Point(jumped.x, jumped.y));
                    }
                }
                path.add(new Point(endX, endY));
                allPaths.add(path);
            }
        }
        return allPaths;
    }

    private void executePath(List<Point> path) {
        Point start = path.get(0);
        Point end = path.get(path.size() - 1);
        Point movePoint = new Point(end.x, end.y);
        movePoint.isEnemyJump = false;
        movePoint.jumpedPieces.clear();
        for (int i = 1; i < path.size() - 1; i++) {
            Point mid = path.get(i);
            if (board[mid.y][mid.x] != 0 && !isAllyPiece(board[mid.y][mid.x])) {
                movePoint.isEnemyJump = true;
                movePoint.jumpedPieces.add(new Point(mid.x, mid.y));
            }
        }
        makeMove(start.x, start.y, end.x, end.y, movePoint);
    }

    private boolean isAllyPiece(int piece) {
        if (piece == 0) return false;
        if (currentPlayer == 1 || currentPlayer == 3) { // Белые
            return piece == 1 || piece == 3;
        } else { // Черные
            return piece == 2 || piece == 4;
        }
    }
    private void findOnlyJumps(int x, int y, List<Point> moves, boolean[][] visited) {
        visited[y][x] = true;
        Set<Point> jumpedPieces = new HashSet<>();

        // Проверяем прыжки во всех направлениях
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                
                // Координаты промежуточной клетки (через которую прыгаем)
                int midX = x + dx;
                int midY = y + dy;
                
                // Координаты целевой клетки
                int targetX = x + dx * 2;
                int targetY = y + dy * 2;
                
                if (isValidJump(x, y, midX, midY, targetX, targetY, jumpedPieces) && !visited[targetY][targetX]) {
                    moves.add(new Point(targetX, targetY));
                }
            }
        }
    }

    private boolean isValidJump(int startX, int startY, int midX, int midY, int targetX, int targetY, Set<Point> jumpedPieces) {
        // Проверяем, что все координаты в пределах доски
        if (!isWithinBounds(midX, midY) || !isWithinBounds(targetX, targetY)) {
            return false;
        }
        
        // Проверяем, что целевая клетка пуста
        if (board[targetY][targetX] != 0) {
            return false;
        }
        
        // Проверяем, не прыгали ли уже через эту шашку
        Point jumpedPiece = new Point(midX, midY);
        if (jumpedPieces.contains(jumpedPiece)) {
            return false;
        }
        
        // Получаем тип шашки, через которую прыгаем
        int jumpedPieceType = board[midY][midX];
        
        // Проверяем, что промежуточная клетка содержит дружественную шашку
        if (jumpedPieceType == 0) {
            return false;
        }
        
        int currentPiece = board[startY][startX];
        
        // Проверяем союзные шашки
        boolean isAlly = false;
        if (currentPiece == 1 || currentPiece == 3) { // Белые шашки
            isAlly = (jumpedPieceType == 1 || jumpedPieceType == 3);
        } else if (currentPiece == 2 || currentPiece == 4) { // Черные шашки
            isAlly = (jumpedPieceType == 2 || jumpedPieceType == 4);
        }
        
        return isAlly;
    }

    private void findJumps(int x, int y, List<Point> moves, boolean[][] visited) {
        // Добавляем Set для отслеживания шашек, через которые уже прыгали
        Set<Point> jumpedPieces = new HashSet<>();
        findJumpsWithTracking(x, y, moves, visited, jumpedPieces);
    }

    private void findJumpsWithTracking(int x, int y, List<Point> moves, boolean[][] visited, Set<Point> jumpedPieces) {
        // Сохраняем текущее состояние доски
        int[][] tempBoard = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(board[i], 0, tempBoard[i], 0, BOARD_SIZE);
        }
        
        // Проверяем прыжки во всех направлениях
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                
                // Координаты промежуточной клетки (через которую прыгаем)
                int midX = x + dx;
                int midY = y + dy;
                
                // Координаты целевой клетки
                int targetX = x + dx * 2;
                int targetY = y + dy * 2;
                
                // Передаем jumpedPieces в isValidJump
                if (isValidJump(x, y, midX, midY, targetX, targetY, jumpedPieces)) {
                    if (!visited[targetY][targetX]) {
                        // Добавляем точку приземления
                        moves.add(new Point(targetX, targetY));
                        
                        // Делаем временный ход
                        int piece = board[y][x];
                        board[y][x] = 0;
                        board[targetY][targetX] = piece;
                        
                        // Помечаем текущую клетку как посещенную
                        visited[targetY][targetX] = true;
                        
                        // Добавляем шашку в список использованных для прыжка
                        Set<Point> newJumpedPieces = new HashSet<>(jumpedPieces);
                        newJumpedPieces.add(new Point(midX, midY));
                        
                        // Ищем следующие прыжки из новой позиции
                        findJumpsWithTracking(targetX, targetY, moves, visited, newJumpedPieces);
                        
                        // Восстанавливаем состояние доски
                        for (int i = 0; i < BOARD_SIZE; i++) {
                            System.arraycopy(tempBoard[i], 0, board[i], 0, BOARD_SIZE);
                        }
                        
                        // Снимаем отметку о посещении для следующих веток поиска
                        visited[targetY][targetX] = false;
                    }
                }
            }
        }
    }

    // Модифицируем класс Point для хранения промежуточных точек
    private static class Point {
        int x, y;
        boolean isEnemyJump;
        List<Point> jumpedPieces; // Список шашек, через которые прыгаем

        Point(int x, int y) {
            this.x = x;
            this.y = y;
            this.isEnemyJump = false;
            this.jumpedPieces = new ArrayList<>();
        }

        // Переопределение метода equals для корректного сравнения
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Point)) return false;
            Point other = (Point) obj;
            return this.x == other.x && this.y == other.y;
        }

        // Переопределение метода hashCode для согласованности с equals
        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }


    private void findEnemyJumps(int x, int y, List<Point> moves, boolean[][] visited) {
        Set<Point> jumpedPieces = new HashSet<>();
        findEnemyJumpsWithTracking(x, y, moves, visited, jumpedPieces);
    }

    private void findEnemyJumpsWithTracking(int x, int y, List<Point> moves, boolean[][] visited, Set<Point> jumpedPieces) {
        int[][] tempBoard = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(board[i], 0, tempBoard[i], 0, BOARD_SIZE);
        }
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                
                int midX = x + dx;
                int midY = y + dy;
                int targetX = x + dx * 2;
                int targetY = y + dy * 2;
                
                if (isValidEnemyJump(x, y, midX, midY, targetX, targetY, jumpedPieces) && !visited[targetY][targetX]) {
                    // Создаем новую точку с информацией о прыжке
                    Point newPoint = new Point(targetX, targetY);
                    newPoint.isEnemyJump = true;
                    newPoint.jumpedPieces.add(new Point(midX, midY));
                    
                    // Если это продолжение цепочки прыжков, добавляем предыдущие прыжки
                    for (Point move : moves) {
                        if (move.x == x && move.y == y) {
                            newPoint.jumpedPieces.addAll(move.jumpedPieces);
                            break;
                        }
                    }
                    
                    moves.add(newPoint);
                    
                    // Помечаем текущую клетку как посещенную
                    visited[targetY][targetX] = true;
                    
                    // Добавляем шашку в список использованных для прыжка
                    Set<Point> newJumpedPieces = new HashSet<>(jumpedPieces);
                    newJumpedPieces.add(new Point(midX, midY));
                    
                    // Временно удаляем вражескую шашку
                    int piece = board[y][x];
                    board[y][x] = 0;
                    board[targetY][targetX] = piece;
                    board[midY][midX] = 0;
                    
                    // Ищем следующие прыжки
                    findEnemyJumpsWithTracking(targetX, targetY, moves, visited, newJumpedPieces);
                    
                    // Восстанавливаем состояние доски
                    for (int i = 0; i < BOARD_SIZE; i++) {
                        System.arraycopy(tempBoard[i], 0, board[i], 0, BOARD_SIZE);
                    }
                    
                    visited[targetY][targetX] = false;
                }
            }
        }
    }

    private boolean isValidEnemyJump(int startX, int startY, int midX, int midY, int targetX, int targetY, Set<Point> jumpedPieces) {
        // Проверяем, что все координаты в пределах доски
        if (!isWithinBounds(midX, midY) || !isWithinBounds(targetX, targetY)) {
            return false;
        }
        
        // Проверяем, что целевая клетка пуста
        if (board[targetY][targetX] != 0) {
            return false;
        }
        
        // Проверяем, не прыгали ли уже через эту шашку
        Point jumpedPiece = new Point(midX, midY);
        if (jumpedPieces.contains(jumpedPiece)) {
            return false;
        }
        
        // Получаем тип шашки, через которую прыгаем
        int jumpedPieceType = board[midY][midX];
        
        // Проверяем, что промежуточная клетка содержит вражескую шашку
        if (jumpedPieceType == 0) {
            return false;
        }
        
        int currentPiece = board[startY][startX];
        
        // Проверяем вражеские шашки (инвертированная логика isAlly)
        boolean isEnemy = false;
        if (currentPiece == 1 || currentPiece == 3) { // Белые шашки
            isEnemy = (jumpedPieceType == 2 || jumpedPieceType == 4); // Проверяем черные
        } else if (currentPiece == 2 || currentPiece == 4) { // Черные шашки
            isEnemy = (jumpedPieceType == 1 || jumpedPieceType == 3); // Проверяем белые
        }
        
        return isEnemy;
    }

    private void initTriangles() {
        triangles = new Triangle[8];

        // Для игрока 1 (WR)
        triangles[1] = new Triangle(1, Triangle.TYPE_CAGE, 2);  // Желтый слева
        triangles[0] = new Triangle(1, Triangle.TYPE_ALTAR, 1);    // Красный справа

        // Для игрока 2 (BR)
        triangles[3] = new Triangle(2, Triangle.TYPE_CAGE, 4);  // Желтый слева
        triangles[2] = new Triangle(2, Triangle.TYPE_ALTAR, 3);    // Красный справа

        // Для игрока 3 (WF)
        triangles[5] = new Triangle(3, Triangle.TYPE_CAGE, 6);  // Желтый слева
        triangles[4] = new Triangle(3, Triangle.TYPE_ALTAR, 5);    // Красный справа

        // Для игрока 4 (BF)
        triangles[7] = new Triangle(4, Triangle.TYPE_CAGE, 8);  // Желтый слева
        triangles[6] = new Triangle(4, Triangle.TYPE_ALTAR, 7);    // Красный справа
    }
    // Методы подсчета очков
    private int getWhiteScore() {
        int initialBlackCount = 18; // Начальное количество черных шашек
        int currentBlackCount = 0;
        
        // Подсчитываем текущее количество черных шашек на доске
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == 2 || board[i][j] == 4) { // BR или BF
                    currentBlackCount++;
                }
            }
        }
        
        // Разница между начальным и текущим количеством - это очки белых
        return initialBlackCount - currentBlackCount;
    }

    private int getBlackScore() {
        int initialWhiteCount = 18; // Начальное количество белых шашек
        int currentWhiteCount = 0;
        
        // Подсчитываем текущее количество белых шашек на доске
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == 1 || board[i][j] == 3) { // WR или WF
                    currentWhiteCount++;
                }
            }
        }
        
        // Разница между начальным и текущим количеством - это очки черных
        return initialWhiteCount - currentWhiteCount;
    }

    public void updateMove() {
        currentMove++;
        // Увеличиваем цикл каждые 4 хода без сброса счетчика
        if ((currentMove - 1) % 4 == 0) {
            currentCycle = (currentMove / 4) + 1;
        }
    }

    private void checkWinCondition() {
        if (isDialogShowing) return;

        String winner = "";
        if (isPlayer1Eliminated && isPlayer3Eliminated) {
            winner = "Победа Чёрных!";
        } else if (isPlayer2Eliminated && isPlayer4Eliminated) {
            winner = "Победа Белых!";
        } else {
            return;
        }

        isDialogShowing = true;
        new AlertDialog.Builder(getContext())
            .setTitle("Игра окончена")
            .setMessage(winner)
            .setPositiveButton("Еще раз", (dialog, which) -> {
                resetGame();
                isDialogShowing = false;
            })
            .setNegativeButton("Выход", (dialog, which) -> {
                ((Activity) getContext()).finish();
                isDialogShowing = false;
            })
            .setOnDismissListener(dialog -> isDialogShowing = false)
            .setCancelable(false)
            .show();
    }

    private void resetGame() {
        // Сбрасываем все состояния
        board = new int[BOARD_SIZE][BOARD_SIZE];
        initializeBoard();
        
        // Сбрасываем флаги выбывания
        isPlayer1Eliminated = false;
        isPlayer2Eliminated = false;
        isPlayer3Eliminated = false;
        isPlayer4Eliminated = false;
        
        // Сбрасываем игровые параметры
        currentPlayer = 1;
        originalSequence = 1;
        currentMove = 1;
        currentCycle = 1;
        
        // Очищаем тюрьмы
        wrPrison.clear();
        brPrison.clear();
        wfPrison.clear();
        bfPrison.clear();
        
        // Сбрасываем параметры вращения
        baseRotation = 45f;
        rotationAngle = 0f;
        
        // Принудительно обновляем представление
        postInvalidate();
    }

    // Метод для нахождения ближайшей к центру шашки для каждого игрока
    private void leaveOnlyCentralPieces() {
        float centerX = BOARD_SIZE / 2f;
        float centerY = BOARD_SIZE / 2f;
        
        // Для каждого игрока
        for (int player = 1; player <= 4; player++) {
            double minDistance = Double.MAX_VALUE;
            int closestX = -1;
            int closestY = -1;

            // Ищем ближайшую к центру шашку
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (board[i][j] == player) {
                        double distance = Math.sqrt(
                            Math.pow(i - centerY, 2) + 
                            Math.pow(j - centerX, 2)
                        );
                        if (distance < minDistance) {
                            minDistance = distance;
                            closestX = j;
                            closestY = i;
                        }
                    }
                }
            }

            // Удаляем все шашки этого игрока, кроме ближайшей к центру
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (board[i][j] == player && (i != closestY || j != closestX)) {
                        board[i][j] = 0;
                    }
                }
            }
        }
        
        invalidate(); // Перерисовываем доску
    }

    // Проверка наличия шашек для каждого игрока
    private void checkPlayerElimination() {
        boolean hasPlayer1Pieces = false;
        boolean hasPlayer2Pieces = false;
        boolean hasPlayer3Pieces = false;
        boolean hasPlayer4Pieces = false;

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                int piece = board[i][j];
                if (piece == 1) hasPlayer1Pieces = true;
                if (piece == 2) hasPlayer2Pieces = true;
                if (piece == 3) hasPlayer3Pieces = true;
                if (piece == 4) hasPlayer4Pieces = true;
            }
        }

        isPlayer1Eliminated = !hasPlayer1Pieces;
        isPlayer2Eliminated = !hasPlayer2Pieces;
        isPlayer3Eliminated = !hasPlayer3Pieces;
        isPlayer4Eliminated = !hasPlayer4Pieces;
    }
}