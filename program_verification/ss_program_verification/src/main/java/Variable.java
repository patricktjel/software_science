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
     * Tries to return the previuos value.
     * If there is no previous value than it will return the current value
     * @return
     */
    public String getPrevious() {
        int size = variables.size();
        if (size == 1) {
            System.out.println("There is no previous");
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
}
