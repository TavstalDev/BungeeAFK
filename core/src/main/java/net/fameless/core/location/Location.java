package net.fameless.core.location;

import com.google.gson.JsonObject;
import net.fameless.core.config.PluginConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Location {

    public static @NotNull Location getConfiguredAfkZone() {
        Map<String, Object> afkZone = PluginConfig.get().getSection("afk-location");
        String worldName = afkZone.get("world").toString();
        double x = Double.parseDouble(afkZone.get("x").toString());
        double y = Double.parseDouble(afkZone.get("y").toString());
        double z = Double.parseDouble(afkZone.get("z").toString());
        return new Location(worldName, x, y, z);
    }

    private String worldName;
    private double x;
    private double y;
    private double z;
    private float pitch = 0.0f;
    private float yaw = 0.0f;

    public Location(String worldName, double x, double y, double z, float pitch, float yaw) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public Location(String worldName, double x, double y, double z) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public JsonObject getAsJsonObject() {
        JsonObject obj = new JsonObject();
        obj.addProperty("worldName", worldName);
        obj.addProperty("x", x);
        obj.addProperty("y", y);
        obj.addProperty("z", z);
        obj.addProperty("pitch", pitch);
        obj.addProperty("yaw", yaw);
        return obj;
    }

    public Map<String, Object> getAsMap() {
        return Map.of(
                "world", worldName,
                "x", x,
                "y", y,
                "z", z,
                "pitch", pitch,
                "yaw", yaw
        );
    }

    @Override
    public String toString() {
        return getAsJsonObject().toString();
    }
}
