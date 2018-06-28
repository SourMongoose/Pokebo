package com.chrisx.pokebo;

import android.graphics.Canvas;

import java.util.List;

import static java.util.Collections.shuffle;

class Player {
    private List<Card> cards;
    private int points, character;

    Player(int character) {
        this.character = character;

        cards = MainActivity.starterDeck();
        shuffle(cards);

        points = 0;
    }
    Player(int character, List<Card> cards) {
        this.character = character;

        this.cards = cards;
        shuffle(this.cards);

        points = 0;
    }

    void addPoint() {
        points++;
    }
    int getPoints() {
        return points;
    }

    Card play(int i) {
        Card c = cards.get(i);
        cards.remove(i);
        cards.add(c);
        return c;
    }

    void drawHand() {
        Canvas c = MainActivity.canvas;

        float margin = MainActivity.c480(80) / 6;
        float w = MainActivity.c480(112), h = MainActivity.c480(80);
        for (int i = 0; i < 5; i++) {
            cards.get(i).draw(margin,margin+i*(h+margin),margin+w,(i+1)*(h+margin));
        }
    }
}
