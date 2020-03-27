package com.memorize;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Random;

class Card {
    Paint p = new Paint();

    public Card(float x, float y, float width, float height, int color) {
        this.color = color;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    int color, backColor = Color.DKGRAY;
    boolean isOpen = false; // цвет карты
    float x, y, width, height;

    public void draw(Canvas c) {
        // нарисовать карту в виде цветного прямоугольника
        if (isOpen) {
            p.setColor(color);
        } else p.setColor(backColor);
        c.drawRect(x,y, x+width, y+height, p);
    }

    public boolean flip (float touch_x, float touch_y) {
        if (touch_x >= x && touch_x <= x + width && touch_y >= y && touch_y <= y + height) {
            isOpen = ! isOpen;
            return true;
        } else return false;
    }

}

public class TilesView extends View {
    final int PAUSE_LENGTH = 1;
    boolean isOnPauseNow = false;

    int openedCard = 0;
    int n = 4;
    int koef = 2;
    int cardsSize = koef*n;
    ArrayList<Card> cards;
    ArrayList<Integer> tiles = new ArrayList<>();

    int width, height; // ширина и высота канвы

    public TilesView(Context context) {
        super(context);
        initializeTiles();
    }

    public TilesView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initializeTiles();
    }

    public void initializeTiles() {
        Random random = new Random();

        for (int i = 0; i < cardsSize/2; i++) {
            int red = random.nextInt(256);
            int green = random.nextInt(256);
            int blue = random.nextInt(256);

            tiles.add(Color.rgb(red, green, blue));
            tiles.add(Color.rgb(red, green, blue));
        }

        int tileSize = tiles.size();
        for (int i = 0; i < tileSize; i++) {
            int randomIndexToSwap = random.nextInt(tileSize);
            int temp = tiles.get(randomIndexToSwap);
            tiles.set(randomIndexToSwap, tiles.get(i));
            tiles.set(i, temp);
        }
    }

    public void initializeCards(int width) {
        int rect_width = (width - 40) / 3;
        int rect_height = rect_width + 100;
        int x = 0;
        int y = 0;

        cards = new ArrayList<>();
        for (int i = 0; i < cardsSize; i ++) {
            cards.add(new Card(x, y, rect_width, rect_height, tiles.get(i)));
            x += rect_width + 20;
            if ((x + rect_width) >= width) {
                x = 0;
                y += rect_height + 20;
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        initializeCards(getWidth());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        width = canvas.getWidth();
        height = canvas.getHeight();

        for (Card c: cards) {
            c.draw(canvas);
        }
    }

    private ArrayList<Card> getEqualOpenCards() {
        ArrayList<Integer> currentColors = new ArrayList<>();
        ArrayList<Card> currentCards = new ArrayList<>();

        for (Card c : cards) {
            if ((currentColors.size() == 0) && c.isOpen) {
                currentColors.add(c.color);
                currentCards.add(c);
            } else if ((currentColors.size() == 1) && c.isOpen && (currentColors.get(0) == c.color)) {
                currentCards.add(c);
            }
        }

        if (currentCards.size() == 2) {
            return currentCards;
        } else {
            return null;
        }
    }

    private void deleteEqualCards(ArrayList<Card> equalCards) {
        for (Card c : equalCards) {
            cards.remove(c);
        }
    }

    public void newGame() {
        cards.clear();
        tiles.clear();

        initializeTiles();
        initializeCards(width);
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            for (Card c : cards) {

                if (openedCard == 0) {
                    if (c.flip(x, y)) {
                        openedCard++;
                        invalidate();
                        return true;
                    }
                }

                if (openedCard == 1) {
                    // перевернуть карту с задержкой
                    if (c.flip(x, y)) {
                        openedCard++;

                        invalidate();
                        PauseTask task = new PauseTask();
                        task.execute(PAUSE_LENGTH);
                        isOnPauseNow = true;

                        return true;
                    }
                }
            }
        }
        return true;
    }

    class PauseTask extends AsyncTask<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... integers) {
            try {
                Thread.sleep(integers[0] * 1000); // передаём число секунд ожидания
            } catch (InterruptedException e) {}
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //delete same cards
            ArrayList<Card> equalCards = getEqualOpenCards();
            if (equalCards != null) {
                deleteEqualCards(equalCards);
                invalidate();
            }

            if (cards.size() == 0) {
                Toast toast = Toast.makeText(getContext(),
                        "You win", Toast.LENGTH_LONG);
                toast.show();
            }

            for (Card c: cards) {
                if (c.isOpen) {
                    c.isOpen = false;
                }
            }
            openedCard = 0;
            isOnPauseNow = false;
            invalidate();
        }
    }
}