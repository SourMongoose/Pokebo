package com.chrisx.pokebo;

/**
 * Organized in order of priority:
 * @TODO everything
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private Bitmap bmp;
    static Canvas canvas;
    private LinearLayout ll;
    private float scaleFactor;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    private GoogleSignInAccount acc;
    private GoogleSignInOptions gsio;
    private GoogleSignInClient gsic;
    private final int RC_SIGN_IN = 1234;

    static int N[] = {33, 52, 89};
    static Bitmap[][] sprites;
    static Bitmap[] icons, cards;
    static Bitmap pokebo, deck, gplay, loggedin;

    static Typeface font;

    private boolean paused = false;
    private long frameCount = 0;

    private String menu = "start";

    //frame data
    private static final int FRAMES_PER_SECOND = 60;
    private long nanosecondsPerFrame;

    private int TRANSITION_MAX = FRAMES_PER_SECOND * 2 / 3;
    private int transition = TRANSITION_MAX / 2;

    private float downX, downY;

    private Paint play;

    private List<Card> playerDeck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set up Google Play sign-in
        gsio = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                .requestEmail().build();
        gsic = GoogleSignIn.getClient(this, gsio);

        //creates the bitmap
        //note: Star 4.5 is 480x854
        int targetH = 480,
                wpx = getAppUsableScreenSize(this).x,
                hpx = getAppUsableScreenSize(this).y;
        scaleFactor = Math.min(1,(float)targetH/hpx);
        bmp = Bitmap.createBitmap(Math.round(wpx*scaleFactor),Math.round(hpx*scaleFactor),Bitmap.Config.RGB_565);

        //creates canvas
        canvas = new Canvas(bmp);

        ll = (LinearLayout) findViewById(R.id.draw_area);
        ll.setBackgroundDrawable(new BitmapDrawable(bmp));

        //initializes SharedPreferences
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        Resources res = getResources();

        sprites = new Bitmap[3][Math.max(N[0],Math.max(N[1],N[2]))];
        for (int i = 0; i < N[0]; i++) {
            sprites[0][i] = BitmapFactory.decodeResource(res, R.drawable.fire00+i);
        }
        for (int i = 0; i < N[1]; i++) {
            sprites[1][i] = BitmapFactory.decodeResource(res, R.drawable.grass00+i);
        }
        for (int i = 0; i < N[2]; i++) {
            sprites[2][i] = BitmapFactory.decodeResource(res, R.drawable.water00+i);
        }
        icons = new Bitmap[3];
        for (int i = 0; i < icons.length; i++) {
            icons[i] = BitmapFactory.decodeResource(res, R.drawable.icon_fire+i);
        }
        cards = new Bitmap[3];
        for (int i = 0; i < cards.length; i++) {
            cards[i] = BitmapFactory.decodeResource(res, R.drawable.card_fire+i);
        }

        pokebo = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.pokebo),
                Math.round(c480(304)),Math.round(c480(118)),false);
        deck = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.deck),
                Math.round(c480(60)),Math.round(c480(60)),false);
        gplay = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.googleplay),
                Math.round(c480(60)),Math.round(c480(60)),false);
        loggedin = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.loggedin),
                Math.round(c480(60)),Math.round(c480(60)),false);

        nanosecondsPerFrame = (long)1e9 / FRAMES_PER_SECOND;

        //initialize fonts
        font = Typeface.createFromAsset(getAssets(), "fonts/Exo2-SemiBold.ttf");

        canvas.drawColor(Color.BLACK);

        //pre-defined paints
        play = newPaint(Color.WHITE);
        play.setTextAlign(Paint.Align.CENTER);
        play.setTextSize(c480(75));

        playerDeck = starterDeck();


        final Handler handler = new Handler();

        //Update thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                //draw loop
                while (!menu.equals("quit")) {
                    final long startTime = System.nanoTime();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!paused) {

                                //fading transition effect
                                if (transition > 0) {
                                    transition--;
                                }
                            }
                        }
                    });

                    frameCount++;

                    //wait until frame is done
                    while (System.nanoTime() - startTime < nanosecondsPerFrame);
                }
            }
        }).start();

        //UI thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                //draw loop
                while (!menu.equals("quit")) {
                    final long startTime = System.nanoTime();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!paused) {
                                if (transition < TRANSITION_MAX / 2) {
                                    canvas.drawColor(Color.rgb(100, 200, 100));

                                    if (menu.equals("start")) {
                                        drawTitleMenu();
                                    }
                                }

                                //fading transition effect
                                if (transition > 0) {
                                    int t = TRANSITION_MAX / 2, alpha;
                                    if (transition > t) {
                                        alpha = 255 - 255 * (transition - t) / t;
                                    } else {
                                        alpha = 255 - 255 * (t - transition) / t;
                                    }
                                    canvas.drawColor(Color.argb(alpha, 20, 20, 20));
                                }

                                //update canvas
                                ll.invalidate();
                            }
                        }
                    });

                    //wait until frame is done
                    while (System.nanoTime() - startTime < nanosecondsPerFrame);
                }
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;

        //try to sign in to Google Play
        if (!isSignedIn()) signInSilently();
    }

    @Override
    public void onBackPressed() {
        if (menu.equals("deck")) {
            goToMenu("start");
        }
    }

    @Override
    //handles touch events
    public boolean onTouchEvent(MotionEvent event) {
        float X = event.getX()*scaleFactor;
        float Y = event.getY()*scaleFactor;
        int action = event.getAction();

        if (menu.equals("start")) {
            if (action == MotionEvent.ACTION_DOWN) {
                Rect sp = new Rect(), mp = new Rect();
                play.getTextBounds("SINGLEPLAYER",0,12,sp);
                play.getTextBounds("MULTIPLAYER",0,11,mp);

                //view deck
                if (X < c480(100) && Y < c480(100)) {
                    goToMenu("deck");
                }
                //sign in to google play
                else if (X > w()-c480(100) && Y < c480(100)) {
                    if (isSignedIn()) signOut();
                    else startSignInIntent();
                }
                //singleplayer mode
                else if (X > w()/2-sp.width()/2 && X < w()/2+sp.width()/2
                        && Y > h()*4/6-sp.height() && Y < h()*4/6) {
                    goToMenu("singleplayer");
                }
                //multiplayer mode
                else if (X > w()/2-mp.width()/2 && X < w()/2+mp.width()/2
                        && Y > h()*5/6-mp.height() && Y < h()*5/6) {
                    if (isSignedIn()) goToMenu("multiplayer");
                }
            }
        }

        return true;
    }

    private boolean isSignedIn() {
        return GoogleSignIn.getLastSignedInAccount(this) != null;
    }

    private void signInSilently() {
        gsic.silentSignIn().addOnCompleteListener(this,
                new OnCompleteListener<GoogleSignInAccount>() {
                    @Override
                    public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                        if (task.isSuccessful()) {
                            // The signed in account is stored in the task's result.
                            acc = task.getResult();
                        } else {
                            // Player will need to sign-in explicitly using via UI
                        }
                    }
                });
    }

    private void startSignInIntent() {
        Intent intent = gsic.getSignInIntent();
        startActivityForResult(intent, RC_SIGN_IN);
    }

    private void signOut() {
        gsic.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // at this point, the user is signed out.
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // The signed in account is stored in the result.
                acc = result.getSignInAccount();
            } else {
                String message = result.getStatus().getStatusMessage();
                if (message == null || message.isEmpty()) {
                    message = getString(R.string.signin_other_error);
                }
                new AlertDialog.Builder(this).setMessage(message)
                        .setNeutralButton(android.R.string.ok, null).show();
            }
        }
    }

    private Point getAppUsableScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    //shorthand for w() and h()
    static float w() {
        return canvas.getWidth();
    }
    static float h() {
        return canvas.getHeight();
    }

    //creates an instance of Paint set to a given color
    static Paint newPaint(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTypeface(font);

        return p;
    }

    static int power(int type, int id) {
        int n = N[type];
        for (int j = 1; j < 13; j++)
            if (id*1./n < j*(j+1)/2*1./(12*13/2))
                return 13 - j;
        return 1;
    }

    static int typeToInt(String t) {
        return t.equals("fire") ? 0 : t.equals("grass") ? 1 : 2;
    }

    static float c480(float f) {
        return h() / (480 / f);
    }
    static float c854(float f) {
        return w() / (854 / f);
    }

    private int getCharacter() {
        return sharedPref.getInt("character", -1);
    }

    private long getHighScore() {
        return sharedPref.getInt("high_score", 0);
    }

    private double toRad(double deg) {
        return Math.PI/180*deg;
    }

    private void goToMenu(String s) {
        transition = TRANSITION_MAX;

        menu = s;
    }

    private Card getCard(int type, int p) {
        int id;
        do {
            id = (int)(Math.random()*N[type]);
        } while (power(type,id) != p);
        return new Card(type,id);
    }

    private List<Card> starterDeck() {
        List<Card> list = new ArrayList<>();

        //For each type:
        for (int type = 0; type < 3; type++) {
            //Add one card each of power 2, 3-4, and 5-6
            int a = 2, b = (int)(Math.random()*2+3), c = (int)(Math.random()*2+5);

            list.add(getCard(type,a));
            list.add(getCard(type,b));
            list.add(getCard(type,c));
        }

        return list;
    }

    private void drawTitleMenu() {
        //title
        canvas.drawBitmap(pokebo,w()/2-pokebo.getWidth()/2,h()/3-pokebo.getHeight()/2,null);

        //play
        play.setAlpha(255);
        canvas.drawText("SINGLEPLAYER",w()/2,h()*4/6,play);
        if (!isSignedIn()) play.setAlpha(50);
        canvas.drawText("MULTIPLAYER",w()/2,h()*5/6,play);

        //view deck
        canvas.drawBitmap(deck,c480(20),c480(20),null);
        //sign in/out
        if (isSignedIn()) canvas.drawBitmap(loggedin,w()-c480(80),c480(20),null);
        canvas.drawBitmap(gplay,w()-c480(80),c480(20),null);
    }
}
