package unseen.game;

import unseen.items.Item;

public class RewardChoice {
    private final String name;
    private final Item item;

    public RewardChoice(String name, Item item) {
        this.name = name;
        this.item = item;
    }

    public String getName() {
        return name;
    }

    public Item getItem() {
        return item;
    }
}
