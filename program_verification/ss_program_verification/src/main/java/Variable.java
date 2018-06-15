import java.util.ArrayList;
import java.util.List;

public class Variable {
    private final String type;
    private final String name;

    private List<String> variables;

    public Variable (String name, String type) {
        this.variables = new ArrayList<>();
        this.name = name;
        this.type = type;

        variables.add(name + "_0");
    }

    public String getNext() {
        String next = name + "_" + variables.size();
        variables.add(next);
        return next;
    }

    public String getCurrent() {
        return variables.get(variables.size() - 1);
    }

    /**
     * Tries to return the previous value.
     * If there is no previous value than it will return the current value
     * @return
     */
    public String getPrevious() {
        int size = variables.size();
        if (size == 1) {
            return this.getCurrent();
        } else {
            return variables.get(variables.size() - 2);
        }
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public List<String> getVariables() {
        return variables;
    }

    public List<String> copyVariables() {
        return new ArrayList<>(variables);
    }

    /**
     * Delete all items from the end of the list till you find name_s
     * In which name is the name of this variable and s is an int.
     * @param s
     */
    public void resetTo(int s) {
        boolean found = false;
        for (int i = variables.size() - 1; !found && i >= 0; i--) {
            if (!variables.get(i).equals(this.name + "_" + s)) {
                variables.remove(i);
            } else {
                found = true;
            }
        }
    }

    public void createTill(int till) {
        if (till < variables.size()) {
            return;
        }

        while (variables.size() <= till) {
            getNext();
        }
    }
}
