/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * PropertiesPanel.java
 *
 * Created on 1-ott-2010, 11.57.05
 */
package it.freedomotic.util;

import it.freedomotic.util.SpringUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

/**
 *
 * @author enrico
 */
public class PropertiesPanel extends javax.swing.JPanel {

    private boolean isKeyEditable = true;
    private ArrayList<Component> componentKey = new ArrayList<Component>();
    private ArrayList<Component> componentValue = new ArrayList<Component>();

    /** Creates new form PropertiesPanel */
    public PropertiesPanel() {
        initComponents();
//        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setVisible(true);
    }

    public void addRow(String key, String value) {
        if ((key == null ? "" != null : !key.isEmpty())
                && (value == null ? "" != null : !value.isEmpty())) {
            JTextField keyComp = new JTextField(key, JLabel.TRAILING);
            JTextField valueComp = new JTextField(value, JLabel.TRAILING);
            componentKey.add(keyComp);
            add(keyComp);
            componentValue.add(valueComp);
            add(valueComp);
            //update the panel
            layoutPanel();
        }
    }

    /**
     * Adds an empty row to the list (no text in row components)
     */
    public void addRow() {
        //add the key
        JTextField keyComp = new JTextField("", JLabel.TRAILING);
        JTextField valueComp = new JTextField("", JLabel.TRAILING);
        keyComp.setMaximumSize(new Dimension(99999, 20));
        valueComp.setMaximumSize(new Dimension(99999, 20));
        componentKey.add(keyComp);
        add(keyComp);
        componentValue.add(valueComp);
        add(valueComp);
        //update the panel
        layoutPanel();
    }

    public void addRow(String key, ArrayList<String> value) {
        JTextField keyComp = new JTextField(key, JLabel.TRAILING);
        keyComp.setMaximumSize(new Dimension(99999, 20));
        componentKey.add(keyComp);
        add(keyComp);

        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (String string : value) {
            model.addElement(string);
        }
        JComboBox valueComp = new JComboBox();
        valueComp.setModel(model);
        valueComp.setMaximumSize(new Dimension(99999, 20));
        componentValue.add(valueComp);
        add(valueComp);
        layoutPanel();
    }

    public void addRow(String key, Component valueComp) {
        JTextField keyComp = new JTextField(key, JLabel.TRAILING);
        keyComp.setMaximumSize(new Dimension(99999, 20));
        valueComp.setMaximumSize(new Dimension(99999, 20));
        componentKey.add(keyComp);
        add(keyComp);
        componentValue.add(valueComp);
        add(valueComp);
        layoutPanel();
    }

    private void layoutPanel() {
        int numOfRows = componentKey.size();
        if (!isKeyEditable) {
            for (Component comp : componentKey) {
                comp.setEnabled(false);
            }
        }
        this.setLayout(new SpringLayout());
        //Lay out the panel.
        SpringUtilities.makeGrid(this,
                numOfRows, 2, //rows, cols
                3, 3, //initX, initY
                3, 3);       //xPad, yPad

    }

    public void setKeysEditable(boolean editable) {
        isKeyEditable = editable;
    }

    /**
     * Used to get an entry set of the key and values in input
     * You can iterate using
     * for (Entry e : propertiesPanel.getProperties()){
     *    //do something... e.getKey or e.getValue()
     * }
     * @return Set<Entry<Object, Object>>
     */
    public Set<Entry<Object, Object>> getProperties() {
        Properties param = new Properties();
        for (int i = 0; i < componentKey.size(); i++) {
            String key = null, value = null;
            if (componentKey.get(i) instanceof JTextField) {
                JTextField field = (JTextField) componentKey.get(i);
                key = field.getText();
            }
            if (componentValue.get(i) instanceof JTextField) {
                JTextField field = (JTextField) componentValue.get(i);
                value = field.getText();
            }
            param.setProperty(key, value);
        }
        return param.entrySet();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}