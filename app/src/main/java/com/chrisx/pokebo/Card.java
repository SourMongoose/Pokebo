package com.chrisx.pokebo;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

class Card {
    private float left, top, right, bottom;
    private int type, id, power;
    private Bitmap pokemon, icon, card;

    Card(int type, int id, float l, float t, float r, float b) {
        this.type = type;
        this.id = id;

        //bitmaps
        pokemon = MainActivity.sprites[type][id];
        icon = MainActivity.icons[type];
        card = MainActivity.cards[type];

        //assign card power
        int N = MainActivity.N[type];
        for (int j = 1; j < 13; j++) {
            if (id*1./N < j*(j+1)/2*1./(12*13/2)) {
                power = 13 - j;
                break;
            }
        }

        left = l;
        top = t;
        right = r;
        bottom = b;
    }

    int getPower() {
        return power;
    }
    int getType() {
        return type;
    }

    int compare(Card c) {
        int a = type, b = c.getType();

        if ((a+1)%3 == b) return 1;
        if ((b+1)%3 == a) return -1;
        return power - c.getPower();
    }

    void draw() {
        Canvas c = MainActivity.canvas;

        float sideW = (right - left) - (bottom - top);

        c.drawBitmap(card, null, new RectF(left,top,right,bottom), null);
        c.drawBitmap(pokemon, null, new RectF(left+sideW,top,right,bottom), null);
        c.drawBitmap(icon, null, new RectF(left,(bottom+top)/2,left+sideW,(bottom+top)/2+sideW), null);

        Paint p = MainActivity.newPaint(Color.BLACK);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(sideW*.8f);
        c.drawText(power+"",left+sideW/2,(bottom+top)/2-sideW*.2f,p);
    }
}
