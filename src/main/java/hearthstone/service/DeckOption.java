package hearthstone.service;

public record DeckOption(String code, String name, String description) {

    @Override
    public String toString() {
        return name;
    }
}
