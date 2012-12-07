package de.pk86.bf.pl;


import java.sql.SQLException;
import java.util.Stack;

import org.apache.log4j.Logger;

/**
 * @deprecated
 * Transaction Manager
 */
final class BfTransactionManager {
	private static final Logger logger = Logger.getLogger(BfTransactionManager.class);


	private BfConnection databaseConnection;
	private Stack<String> transactions = new Stack<String>(); // sync!!
	private boolean isRollback = false;
	private boolean isCommited = false;
	private Thread owner;
	// private long timeStarted;

	private int status = NEW;
	private static final int NEW = 0;
	private static final int STARTED = 1;
	private static final int COMMITTED = 2;
	private static final int ROLLBACKED = 3;
	private static final int ABORTED = 4;

	BfTransactionManager(BfConnection databaseConnection) {
		this.databaseConnection = databaseConnection;
	}

//	private BfDatabase getDatabase() {
//		BfDatabase db = this.databaseConnection.getDatabase();
//		return db;
//	}

	private void checkOwner(String label) {
		if (this.owner != Thread.currentThread()) {
			String msg = "[" + label + "] Current Thread: " + Thread.currentThread().getName()
			      + " not owner of TransactionManager: " + owner.getName();
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
	}

	void startTransaction(String transName) {
		if (transName == null) {
			throw new IllegalArgumentException("PL [ " + databaseConnection.getLayerName() + " ] "
			      + "Transactionname is NULL");
		}
		transName = transName + " " + Thread.currentThread().getName();
		synchronized (transactions) {
			if (transactions.size() == 0) {
				this.owner = Thread.currentThread();
				this.status = STARTED;
				// this.timeStarted = System.currentTimeMillis();
			} else {
				this.checkOwner("StartTrans: " + transName);
			}
			this.isCommited = false;
			isRollback = false;
			this.transactions.push(transName);
		}
	}

	boolean testCommit() {
		synchronized (transactions) {
			if (this.transactions.size() > 0 && isRollback == false) {
				return true;
			} else {
				return false;
			}
		}
	}

	boolean isCommited() {
		return isCommited;
	}

	boolean isRollback() {
		synchronized (transactions) {
			return isRollback && transactions.size() == 0;
		}
	}

	/**
	 * Beendet die Transaktion
	 * 
	 * @param transName
	 * @return 'true' wenn Transaktion wirklich beendet wurde; ansonsten 'false',
	 *         wenn geschachtelte Transaktion.
	 * @throws BfException
	 */
	boolean commitTransaction(String transName) throws BfException {
		transName = transName + " " + Thread.currentThread().getName();
		synchronized (transactions) {
			if (this.status != STARTED) {
				throw new BfException("No transaction started");
			}
			this.checkOwner("commitTrans: " + transName);
			String t = transactions.peek();
			if (t.equals(transName) == false) {
				String msg = "PL [ " + databaseConnection.getLayerName() + " ] " + "Transaction [ "
				      + transName + " ] can't be commited because the transaction [ " + t
				      + " ] is still running!";
				logger.error(msg);
				throw new BfException(msg);
			}
			if (isRollback == true) {
				String msg = "PL [ " + databaseConnection.getLayerName() + " ] "
				      + "Can't commit Transaction [ " + transName + " ]: Nested rollback!";
				logger.error(msg);
				throw new BfException(msg);
			}
			transactions.pop();
			if (transactions.size() == 0) {
				try {
					databaseConnection.getConnection().commit();
					this.isRollback = false;
					this.isCommited = true;
					this.status = COMMITTED;
					// long duration = System.currentTimeMillis() - this.timeStarted;
					// logger.debug(transName + ":" + duration);
				} catch (SQLException ex) {
					this.isRollback = true;
					this.isCommited = false;
					String msg = "PL [ " + databaseConnection.getLayerName() + " ] "
					      + "Error commiting transaction [ " + transName + " ]: " + ex.getMessage();
					logger.error(msg, ex);
					throw new BfException(msg, ex);
				}
			}
			return this.isCommited;
		}
	}

	boolean rollbackTransaction(String transName) throws BfException {
		// TODO: Eigentlich müßte ein Rollback doch immer die ganze Transaktion
		// beenden??!!
		transName = transName + " " + Thread.currentThread().getName();
		synchronized (transactions) {
			if (this.status != STARTED) {
				throw new BfException("No transaction started");
			}
			this.checkOwner("rollbackTrans:" + transName);
			String t = transactions.peek();
			if (t.equals(transName) == false) {
				String msg = "PL [ " + databaseConnection.getLayerName() + " ] " + "Transaction [ "
				      + transName + " ] can't be rollbacked because the transaction [ " + t
				      + " ] is operating!";
				logger.error(msg);
				throw new BfException(msg);
			}
			transactions.pop();
			if (transactions.size() == 0) {
				this.isRollback = true;
				try {
					databaseConnection.getConnection().rollback();
					this.status = ROLLBACKED;
				} catch (SQLException ex) {
					String msg = "PL [ " + databaseConnection.getLayerName() + " ] "
					      + "Can't rollback transaction Name: " + transName;
					logger.error(msg, ex);
					throw new BfException(msg, ex);
				}
			} else {
				// TODO: Eigentlich müßte ein Rollback doch immer die ganze
				// Transaktion beenden??!!
			}
			return this.isRollback;
		}
	}

	boolean abortTransaction(String transName) throws BfException {
		transName = transName + " " + Thread.currentThread().getName();
		boolean aborted = false;
		synchronized (transactions) {
			if (this.status != STARTED) {
				throw new BfException("No transaction started");
			}
			if (transactions.size() != 0) {
				while (this.transactions.size() > 0) {
					String t = transactions.peek();
					logger.debug("PL [ " + databaseConnection.getLayerName() + " ] "
					      + "TransactionManager: Aborting Transaction: " + t);
					this.transactions.pop();
					aborted = true;
				}
				this.isRollback = false;
				this.status = ABORTED;
			}
		}
		return aborted;
	}

	/**
	 * Liefert true, wenn noch eine Transaction offen ist.
	 * <p>
	 * Dient der Prüfung, ob der TransactionManger in einem 'sauberen' Zustand
	 * ist.
	 * 
	 * @return
	 */
	boolean hasOpenTransaction() {
		synchronized (transactions) {
			int size = transactions.size();
			if (size == 0) {
				return false;
			} else {
				// logger.debug(transactions.toString());
				return true;
			}
		}
	}

	String getOpenTransactions() {
		if (this.hasOpenTransaction() == false) {
			return null;
		}
		StringBuilder ret = new StringBuilder();
		for (int i = 0; i < transactions.size(); i++) {
			String transName = this.transactions.get(i);
			ret.append(transName);
			ret.append("\n");
		}
		return ret.toString();
	}

	int getStatus() {
		return this.status;
	}
}
