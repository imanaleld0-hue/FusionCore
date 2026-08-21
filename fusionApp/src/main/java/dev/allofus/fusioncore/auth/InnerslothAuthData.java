package dev.allofus.fusioncore.auth;
  
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.json.JSONObject;

public final class InnerslothAuthData implements Parcelable {

    public final String store;      
    public final String token;      
    public final String mergeId;   
    public final String sub;        
    public final String name;      
    public final String givenName;
    public final String familyName;
    public final String picture;   
    public final String email;
    public final long iat; 
    public final long exp;
    public final long savedAt;     
    
    public InnerslothAuthData (String store, String token, String mergeId,
                               String sub, String name, String givenName,
                               String familyName, String picture, String email,
                               long iat, long exp) {
        this.store = store;
        this.token = token;
        this.mergeId = mergeId;
        this.sub = sub;
        this.name = name;
        this.givenName = givenName;
        this.familyName = familyName;
        this.picture = picture;
        this.email = email;
        this.iat = iat;
        this.exp = exp;
        this.savedAt = System.currentTimeMillis() / 1000;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() / 1000 >= exp;
    }

    public boolean isValid() {
        return token != null && !token.isEmpty()
            && sub != null && !sub.isEmpty()
            && exp > 0
            && !isExpired();
    }

    public String getMaskedToken() {
        if (token == null || token.length() <= 12) return "***";
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }


    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("store", store);
            obj.put("token", token);
            obj.put("mergeId", mergeId);
            obj.put("sub", sub);
            obj.put("name", name);
            obj.put("given_name", givenName);
            obj.put("family_name", familyName);
            obj.put("picture", picture);
            obj.put("email", email);
            obj.put("iat", iat);
            obj.put("exp", exp);
            obj.put("savedAt", savedAt);
        } catch (Exception ignored) {}
        return obj;
    }

    public static InnerslothAuthData fromJson(JSONObject obj) {
        if (obj == null) return null;
        return new InnerslothAuthData(
            obj.optString("store", ""),
            obj.optString("token", ""),
            obj.optString("mergeId", ""),
            obj.optString("sub", ""),
            obj.optString("name", ""),
            obj.optString("given_name", ""),
            obj.optString("family_name", ""),
            obj.optString("picture", ""),
            obj.optString("email", ""),
            obj.optLong("iat", 0),
            obj.optLong("exp", 0)
        );
    }



    protected InnerslothAuthData(Parcel in) {
        store = in.readString();
        token = in.readString();
        mergeId = in.readString();
        sub = in.readString();
        name = in.readString();
        givenName = in.readString();
        familyName = in.readString();
        picture = in.readString();
        email = in.readString();
        iat = in.readLong();
        exp = in.readLong();
        savedAt = in.readLong();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(store);
        dest.writeString(token);
        dest.writeString(mergeId);
        dest.writeString(sub);
        dest.writeString(name);
        dest.writeString(givenName);
        dest.writeString(familyName);
        dest.writeString(picture);
        dest.writeString(email);
        dest.writeLong(iat);
        dest.writeLong(exp);
        dest.writeLong(savedAt);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<InnerslothAuthData> CREATOR = new Creator<>() {
        @Override
        public InnerslothAuthData createFromParcel(Parcel in) { return new InnerslothAuthData(in); }
        @Override
        public InnerslothAuthData[] newArray(int size) { return new InnerslothAuthData[size]; }
    };
}
