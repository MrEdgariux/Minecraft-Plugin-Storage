package lt.mredgariux.saugykla.datasets;

import org.bukkit.Location;

import java.util.UUID;

public class Chunk {
    private final UUID id;
    private Location start;
    private Location end;
    private int chunkSize; // Size of the chunk in blocks

    // Constructor for initializing a new chunk
    public Chunk(UUID id, Location start, Location end) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.chunkSize = calculateChunkSize(start, end); // Calculate size upon creation
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public Location getStart() {
        return start;
    }

    public Location getEnd() {
        return end;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    // Setters
    public void setStart(Location start) {
        this.start = start;
        this.chunkSize = calculateChunkSize(start, this.end); // Recalculate size
    }

    public void setEnd(Location end) {
        this.end = end;
        this.chunkSize = calculateChunkSize(this.start, end); // Recalculate size
    }

    // Utility method to calculate chunk size
    private int calculateChunkSize(Location start, Location end) {
        int width = Math.abs(start.getBlockX() - end.getBlockX());
        int height = Math.abs(start.getBlockY() - end.getBlockY());
        int depth = Math.abs(start.getBlockZ() - end.getBlockZ());
        return width * height * depth; // Example calculation
    }

    @Override
    public String toString() {
        return "Chunk{" +
                "id=" + id +
                ", start=" + start +
                ", end=" + end +
                ", chunkSize=" + chunkSize +
                '}';
    }
}

