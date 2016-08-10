import javax.swing.*;
import javax.swing.event.*;
import javax.swing.undo.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Arrays;

/**
 * Notepad: A simple text editor
 * 
 * @author  Pratinav Bagla
 * @version v1.0.0
 *
 * Copyright (c) 2016 Pratinav Bagla (https://pratinav.xyz/)
 * Released under The MIT License (https://github.com/pratinav/notepad/blob/master/LICENSE)
 */
public class Main extends JFrame implements ActionListener, DocumentListener, CaretListener, UndoableEditListener {
  private IO io = new IO();
  private JTextArea textArea;
  private JLabel statusBar;
  private JMenuItem undo, redo;
  private JFileChooser fileChooser = new JFileChooser();

  private String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
  private String[] styleNames = {"Plain", "Bold", "Italic", "Bold Italic"};
  private int[] styleValues = {Font.PLAIN, Font.BOLD, Font.ITALIC, Font.BOLD + Font.ITALIC};

  private boolean isNewFile = true;
  private boolean isFileSaved = false;
  private String currentFontFamily = "Lucida Console";
  private int currentFontStyle = Font.PLAIN;
  private String currentFontStyleName = "Plain";
  private int currentFontSize = 14;
  private File currentFile;
  private String currentFilePath;

  private UndoManager undoManager = new UndoManager();

  /**
   * Constructor
   */
  public Main() {
    init();
  }

  public Main(File file) {
    init();
    loadFile(file);
  }

  /**
   * Builds GUI components
   */
  private void init() {
    // Executes before closing
    addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          if (isFileSaved || confirmClose()) dispose();
        }
      });
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

    setSize(700, 600);
    setTitle("Untitled");
    setLayout(new BorderLayout(0, 0));

    JMenuBar menuBar = new JMenuBar();

    JMenu file = new JMenu("File");
    file.setMnemonic(KeyEvent.VK_F);

    JMenuItem newFile = createMenuItem("New", "new", KeyEvent.VK_N, "ctrl N");
    file.add(newFile);

    JMenuItem open = createMenuItem("Open", "open", KeyEvent.VK_O, "ctrl O");
    file.add(open);

    JMenuItem save = createMenuItem("Save", "save", KeyEvent.VK_S, "ctrl S");
    file.add(save);

    JMenuItem saveAs = createMenuItem("Save As", "saveAs", KeyEvent.VK_A, "ctrl shift S");
    file.add(saveAs);

    JMenuItem print = createMenuItem("Print", "print", KeyEvent.VK_P, "ctrl P");
    file.add(print);

    menuBar.add(file);

    JMenu edit = new JMenu("Edit");
    edit.setMnemonic(KeyEvent.VK_E);

    undo = createMenuItem("Undo", "undo", KeyEvent.VK_U, "ctrl Z");
    undo.setEnabled(false);
    edit.add(undo);

    redo = createMenuItem("Redo", "redo", KeyEvent.VK_R, "ctrl shift Z");
    redo.setEnabled(false);
    edit.add(redo);

    JMenuItem copy = createMenuItem("Copy", "copy", KeyEvent.VK_C, "ctrl C");
    edit.add(copy);

    JMenuItem paste = createMenuItem("Paste", "paste", KeyEvent.VK_V, "ctrl V");
    edit.add(paste);

    JMenuItem cut = createMenuItem("Cut", "cut", KeyEvent.VK_X, "ctrl X");
    edit.add(cut);

    JMenuItem selectAll = createMenuItem("Select All", "selectAll", KeyEvent.VK_A, "ctrl A");
    edit.add(selectAll);

    JMenuItem delete = createMenuItem("Delete", "delete", KeyEvent.VK_DELETE);
    edit.add(delete);

    menuBar.add(edit);

    JMenu format = new JMenu("Format");
    format.setMnemonic(KeyEvent.VK_R);

    JCheckBoxMenuItem lineWrap = createMenuCheckbox("Line Wrap", "lineWrap", KeyEvent.VK_L, "ctrl L");
    format.add(lineWrap);

    JMenuItem setFontSize = createMenuItem("Set Font", "setFont", KeyEvent.VK_S, "ctrl shift F");
    format.add(setFontSize);

    JMenuItem increaseFontSizeItem = createMenuItem("Increase Font Size", "increaseFontSize", KeyEvent.VK_I, "ctrl EQUALS");
    format.add(increaseFontSizeItem);

    JMenuItem decreaseFontSizeItem = createMenuItem("Decrease Font Size", "decreaseFontSize", KeyEvent.VK_D, "ctrl MINUS");
    format.add(decreaseFontSizeItem);

    JMenuItem originalFontSize = createMenuItem("Orignal Font Size", "originalFontSize", KeyEvent.VK_O, "ctrl 0");
    format.add(originalFontSize);

    menuBar.add(format);

    JMenu view = new JMenu("View");
    view.setMnemonic(KeyEvent.VK_V);

    JCheckBoxMenuItem toggleStatusBar = createMenuCheckbox("Status Bar", "statusBar", KeyEvent.VK_SLASH, "ctrl SLASH");
    view.add(toggleStatusBar);

    menuBar.add(view);

    textArea = new JTextArea();
    textArea.setFont(new Font(currentFontFamily, currentFontStyle, currentFontSize));
    textArea.getDocument().addUndoableEditListener(this);
    textArea.getDocument().addDocumentListener(this);
    textArea.addCaretListener(this);
    JScrollPane scrollPane = new JScrollPane(textArea);
    add(scrollPane, BorderLayout.CENTER);

    statusBar = new JLabel(" ", JLabel.RIGHT);
    add(statusBar, BorderLayout.PAGE_END);
    add(menuBar, BorderLayout.PAGE_START);
    setVisible(true);
  }

  /**
   * Handles all action events
   * @param e Action Event
   */
  public void actionPerformed(ActionEvent e) {
    String command = e.getActionCommand();

    switch(command) {
      case "undo":
      try {
        undoManager.undo();
      } catch (Exception ex) {}
      updateUndoItems();
      break;

      case "redo":
      try {
        undoManager.redo();
      } catch (Exception ex) {}
      updateUndoItems();
      break;

      case "selectAll":
      textArea.selectAll();
      break;

      case "copy":
      textArea.copy();
      break;

      case "paste":
      textArea.paste();
      break;

      case "cut":
      textArea.cut();
      break;

      case "delete":
      textArea.replaceSelection("");
      break;

      case "lineWrap":
      JCheckBoxMenuItem lineWrapBox = (JCheckBoxMenuItem) e.getSource();
      textArea.setLineWrap(lineWrapBox.isSelected());
      break;

      case "setFont":
      setFont();
      break;

      case "sampleFontFamily":
      JComboBox familyBox = (JComboBox) e.getSource();
      String family = familyBox.getSelectedItem().toString();
      currentFontFamily = family;
      textArea.setFont(new Font(currentFontFamily, currentFontStyle, currentFontSize));
      break;

      case "sampleFontStyle":
      JComboBox styleBox = (JComboBox) e.getSource();
      String style = styleBox.getSelectedItem().toString();
      currentFontStyleName = style;
      currentFontStyle = styleValues[Arrays.asList(styleNames).indexOf(currentFontStyleName)];
      textArea.setFont(new Font(currentFontFamily, currentFontStyle, currentFontSize));
      break;

      case "increaseFontSize":
      textArea.setFont(new Font(currentFontFamily, currentFontStyle, ++currentFontSize));
      break;

      case "decreaseFontSize":
      if(currentFontSize != 1) textArea.setFont(new Font(currentFontFamily, currentFontStyle, --currentFontSize));
      break;

      case "originalFontSize":
      currentFontSize = 14;
      textArea.setFont(new Font("Lucida Console", Font.PLAIN, currentFontSize));
      break;

      case "statusBar":
      JCheckBoxMenuItem statusBarBox = (JCheckBoxMenuItem) e.getSource();
      statusBar.setVisible(statusBarBox.isSelected());
      break;

      case "new":
      new Main();
      break;

      case "open":
      openFile();
      break;

      case "save":
      saveFile();
      break;

      case "saveAs":
      saveFileAs();
      break;

      case "print":
      printFile();
      break;
    }
  }

  /**
   * Opens file
   */
  public void openFile() {
    int returnVal = fileChooser.showOpenDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File file = fileChooser.getSelectedFile();
      if(isNewFileEmpty()) loadFile(file);
      else new Main(file);
    }
  }

  /**
   * Loads file
   */
  public void loadFile(File file) {
    currentFile = file;
    currentFilePath = currentFile.getPath();
    String fileData = io.read(currentFilePath);
    textArea.setText(fileData);
    undo.setEnabled(false);
    redo.setEnabled(false);
    isNewFile = false;
    isFileSaved = true;
    setTitle(currentFile.getName());
  }

  /**
   * Saves file at existing location
   */
  public void saveFile() {
    if (isNewFile) saveFileAs();
    else if (!isFileSaved) {
      io.write(currentFilePath, textArea.getText());
      isFileSaved = true;
    }
  }

  /**
   * Saves file at new location
   * @return  True if successfull, false if user cancels
   */
  public boolean saveFileAs() {
    File file;
    while(true) {
      if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        file = fileChooser.getSelectedFile();
        if (!file.exists()) break;
        if (JOptionPane.showConfirmDialog(this, "Replace file?", "Save as", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) break;
      } else return false;
    }
    currentFile = file;
    currentFilePath = file.getPath();
    isNewFile = false;
    isFileSaved = false;
    setTitle(currentFile.getName());
    saveFile();
    return true;
  }

  /**
   * Prints file
   */
  public void printFile() {
    if (!(textArea.getText().trim().equals("") && JOptionPane.showConfirmDialog(
        this,
        "File is empty, print anyway?", "Print",
        JOptionPane.YES_NO_OPTION
      ) == JOptionPane.NO_OPTION)) {
      try {
        textArea.print();
      } catch (Exception pe) {}
    }
  }

  /**
   * Sets font from user
   */
  public void setFont() {
    int currentFontFamilyIndex = Arrays.asList(fonts).indexOf(currentFontFamily);
    int currentFontStyleIndex = Arrays.asList(styleNames).indexOf(currentFontStyleName);
    Font font;

    JDialog dialog = new JDialog(this, "Select font");
    dialog.setLayout(new BorderLayout(0, 5));

    JComboBox fontBox = new JComboBox(fonts);
    fontBox.setSelectedIndex(currentFontFamilyIndex);
    fontBox.setActionCommand("sampleFontFamily");
    fontBox.addActionListener(this);
    dialog.add(fontBox, BorderLayout.PAGE_START);

    JComboBox styleBox = new JComboBox(styleNames);
    styleBox.setSelectedIndex(currentFontStyleIndex);
    styleBox.setActionCommand("sampleFontStyle");
    styleBox.addActionListener(this);
    dialog.add(styleBox, BorderLayout.CENTER);

    JTextField fontSizeInput = new JTextField(Integer.toString(currentFontSize));
    fontSizeInput.getDocument().putProperty("owner", fontSizeInput);
    fontSizeInput.getDocument().addDocumentListener(this);
    dialog.add(fontSizeInput, BorderLayout.LINE_END);

    dialog.pack();
    dialog.setVisible(true);
  }

  /**
   * Caret listeners
   */
  public void caretUpdate(CaretEvent e) {
    int line = 1;
    int col = 1;
    try {
      int caretPos = e.getDot();
      line = textArea.getLineOfOffset(caretPos) + 1;
      col = (caretPos - textArea.getLineStartOffset(line - 1)) + 1;
    } catch (Exception ex) {}
    statusBar.setText("Line " + line + ", Column " + col + " ");
  }

  /**
   * Document listeners
   */
  private void onSampleChange(Object source) {
    JTextField sizeInput = (JTextField) source;
    int newSize = currentFontSize;
    try {
      newSize = Integer.parseInt(sizeInput.getText());
    } catch (Exception ex) {}
    currentFontSize = Math.max(1, newSize);
    textArea.setFont(new Font(currentFontFamily, currentFontStyle, currentFontSize));
  }

  private void onAreaChange() {
    if (isFileSaved) isFileSaved = false;
  }

  public void insertUpdate(DocumentEvent e) {
    if (e.getDocument() == textArea.getDocument()) onAreaChange();
    else onSampleChange(e.getDocument().getProperty("owner"));
  }

  public void removeUpdate(DocumentEvent e) {
    if (e.getDocument() == textArea.getDocument()) onAreaChange();
    else onSampleChange(e.getDocument().getProperty("owner"));
  }

  public void changedUpdate(DocumentEvent e) {}

  /**
   * Undoable Edit listener
   */
  public void undoableEditHappened(UndoableEditEvent e) {
    undoManager.addEdit(e.getEdit());
    updateUndoItems();
  }

  /**
   * Sets undo and redo menu items to enabled/disabled
   */
  private void updateUndoItems() {
    undo.setEnabled(undoManager.canUndo());
    redo.setEnabled(undoManager.canRedo());
  }

  /**
   * Checks if file is new and empty
   * @return  true is file is new, and is empty
   */
  private boolean isNewFileEmpty() {
    if (isNewFile && textArea.getText().trim().equals("")) return true;
    return false;
  }

  /**
   * Confirms if user wants to close file without saving
   * @return  true if user saves, false if user cancels
   */
  private boolean confirmClose() {
    if (isNewFileEmpty()) return true;

    int option = JOptionPane.showConfirmDialog(this, "Save file before exiting?", "Save file?", JOptionPane.YES_NO_CANCEL_OPTION);

    if (option == JOptionPane.YES_OPTION) {
      saveFile();
      return true;
    }

    if (option == JOptionPane.NO_OPTION) return true;

    return false;
  }

  /**
   * Creates a new menu item
   */
  private JMenuItem createMenuItem(String content, String actionCommand, int mnemonic, String accelerator) {
    JMenuItem item = new JMenuItem(content, mnemonic);
    item.setAccelerator(KeyStroke.getKeyStroke(accelerator));
    item.setActionCommand(actionCommand);
    item.addActionListener(this);
    return item;
  }

  private JMenuItem createMenuItem(String content, String actionCommand, int mnemonic) {
    JMenuItem item = new JMenuItem(content, mnemonic);
    item.setActionCommand(actionCommand);
    item.addActionListener(this);
    return item;
  }

  /**
   * Creates a new checkbox menu item
   */
  private JCheckBoxMenuItem createMenuCheckbox(String content, String actionCommand, int mnemonic, String accelerator) {
    JCheckBoxMenuItem checkbox = new JCheckBoxMenuItem(content);
    checkbox.setMnemonic(mnemonic);
    checkbox.setAccelerator(KeyStroke.getKeyStroke(accelerator));
    checkbox.setActionCommand(actionCommand);
    checkbox.addActionListener(this);
    return checkbox;
  }

  /**
   * Program entry point
   */
  public static void main(String[] args) {
    new Main();
  }
}