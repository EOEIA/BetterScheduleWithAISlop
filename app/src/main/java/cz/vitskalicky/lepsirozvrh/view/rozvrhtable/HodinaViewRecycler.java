package cz.vitskalicky.lepsirozvrh.view.rozvrhtable;

import android.content.Context;
import cz.vitskalicky.lepsirozvrh.theme.RozvrhTheme;

import java.util.LinkedList;
import java.util.List;

/** Recycles instances of HodinaView */
public class HodinaViewRecycler {
    private Context context;
    public RozvrhTheme theme;
    private List<HodinaView> buffer = new LinkedList<>();

    public HodinaViewRecycler(Context context, RozvrhTheme theme) {
        this.theme = theme;
        this.context = context;
    }

    public void store(HodinaView... items){
        for (HodinaView item :items) {
            item.setHodina(null, false,false);
            item.setOnClickListener(null);
            buffer.add(item);
        }
    }

    public HodinaView retrieve(){
        HodinaView item;
        if (buffer.size() > 0){
            item = buffer.remove(0);
        }else {
            item = new HodinaView(context, null);
        }
        item.setTheme(theme);
        return item;
    }
}
