package chess;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

//regras do jogo
import bordgame.Board;
import bordgame.Piece;
import bordgame.Posicao;
import chess.pieces.Bispo;
import chess.pieces.Cavalo;
import chess.pieces.Peao;
import chess.pieces.Rainha;
import chess.pieces.Rei;
import chess.pieces.Torre;

//partida de xadrez
public class ChessMatch {

	private int turn;
	private Color currentPlayer;
	private Board board;
	private boolean check;
	private boolean checkMate;
	private ChessPiece enPassantVulnerable;
	private ChessPiece promoted;

	private List<Piece> piecesOnTheBoard = new ArrayList<>();
	private List<Piece> capturedPieces = new ArrayList<>();

	public ChessMatch() {
		board = new Board(8, 8);
		turn = 1;
		currentPlayer = Color.WHITE;
		initialSetup();
	}

	public int getTurn() {
		return turn;
	}

	public Color getCurrentPlayer() {
		return currentPlayer;
	}

	public boolean getCheck() {
		return check;
	}

	public boolean getCheckMate() {
		return checkMate;
	}
	public ChessPiece getPromoted() {
		return promoted;
	}
	
	public ChessPiece getEnPassantVulnerable() {
		return enPassantVulnerable;
	}

	public ChessPiece[][] getPieces() {
		ChessPiece[][] mat = new ChessPiece[board.getLinhas()][board.getColunas()];
		for (int i = 0; i < board.getLinhas(); i++) {
			for (int j = 0; j < board.getColunas(); j++) {
				mat[i][j] = (ChessPiece) board.piece(i, j);

			}
		}
		return mat;
	}

	public boolean[][] possibleMoves(ChessPosition sourcePosition) {
		Posicao posicao = sourcePosition.toPosition();
		validateSourcePosition(posicao);
		return board.piece(posicao).possibleMoves();
	}

	public ChessPiece performChessMove(ChessPosition sourcePosition, ChessPosition targetPosition) {
		Posicao source = sourcePosition.toPosition();
		Posicao target = targetPosition.toPosition();
		validateSourcePosition(source);
		validateTargetPosition(source, target);
		Piece capturedPiece = makeMove(source, target);

		if (testCheck(currentPlayer)) {
			undoMove(source, target, capturedPiece);
			throw new ChessException("You cab't put yourself in check");
		}
		
		ChessPiece movedPiece = (ChessPiece)board.piece(target);
		
		//Movimento especial
		promoted = null;
		if(movedPiece instanceof Peao) {
			if((movedPiece.getColor() == Color.WHITE && target.getLinha() == 0) || (movedPiece.getColor() == Color.BLACK && target.getLinha() == 7)){
				promoted = (ChessPiece)board.piece(target);
				promoted = replacePromotedPiece("Q");
			}
		}
		check = (testCheck(opponent(currentPlayer))) ? true : false;
		
		if (testCheckMate(opponent(currentPlayer))) {
			checkMate = true;
		}
		else {
		nextTurn();
		}
		
		if(movedPiece instanceof Peao && (target.getLinha() == source.getLinha() - 2 || target.getLinha() == source.getLinha() +2)) {
			enPassantVulnerable = movedPiece;
		}
		else {
			enPassantVulnerable = null;
		}
		
		return (ChessPiece) capturedPiece;
	}
	
	public ChessPiece replacePromotedPiece(String type) {
		if(promoted == null) {
			throw new IllegalStateException("There is no piece to be promoted");
		}
		if(!type.equals("B") && !type.equals("C") && !type.equals("T") & !type.equals("Q") ) {
			throw new InvalidParameterException("Inavalid type for promotion");
		}
		Posicao pos = promoted.getChessPosition().toPosition();
		Piece p = board.removePiece(pos);
		piecesOnTheBoard.remove(p);
		
		ChessPiece newPiece = newPiece(type, promoted.getColor());
		board.placePiece(newPiece, pos);
		piecesOnTheBoard.add(newPiece);
		
		return newPiece;
	}
	
	private ChessPiece newPiece(String type, Color color) {
		if(type.equals("B")) return new Bispo(board, color);
		if(type.equals("C")) return new Cavalo(board, color);
		if(type.equals("Q")) return new Rainha(board, color);
		return new Torre(board, color);
		
	}
	

	private Piece makeMove(Posicao source, Posicao target) {
		ChessPiece p = (ChessPiece)board.removePiece(source);
		p.increaseMoveCount();
		Piece capturedPiece = board.removePiece(target);
		board.placePiece(p, target);

		if (capturedPiece != null) {
			piecesOnTheBoard.remove(capturedPiece);
			capturedPieces.add(capturedPiece);
		}
		//movimento especial
		 if(p instanceof Rei && target.getColuna() == source.getColuna() + 2) {
			 Posicao sourceT = new Posicao(source.getLinha(), source.getColuna() + 3);
			 Posicao targetT = new Posicao(source.getLinha(), source.getColuna() + 1);
			 ChessPiece torre = (ChessPiece)board.removePiece(sourceT);
			 board.placePiece(torre, targetT);
			 torre.increaseMoveCount();
			 
		 }
		 
			//movimento especial
		 if(p instanceof Rei && target.getColuna() == source.getColuna() - 2) {
			 Posicao sourceT = new Posicao(source.getLinha(), source.getColuna() - 4);
			 Posicao targetT = new Posicao(source.getLinha(), source.getColuna() - 1);
			 ChessPiece torre = (ChessPiece)board.removePiece(sourceT);
			 board.placePiece(torre, targetT);
			 torre.increaseMoveCount();
			 
		 }
		 
		 //Movimento especial
		 if(p instanceof Peao) {
			if(source.getColuna() != target.getColuna() && capturedPiece == null) {
				Posicao peaoPosicao;
				if(p.getColor() == Color.WHITE) {
					peaoPosicao = new Posicao(target.getLinha() + 1, target.getColuna());
				}
				else{
					peaoPosicao = new Posicao(target.getLinha() - 1, target.getColuna());
				}
				capturedPiece = board.removePiece(peaoPosicao);
				capturedPieces.add(capturedPiece);
				piecesOnTheBoard.remove(capturedPiece);
			}
		 }


		return capturedPiece;
	}

	private void undoMove(Posicao source, Posicao target, Piece capturedPiece) {
		ChessPiece p = (ChessPiece)board.removePiece(target);
		p.decreaseMoveCount();
		board.placePiece(p, source);

		if (capturedPiece != null) {
			board.placePiece(capturedPiece, target);
			capturedPieces.remove(capturedPiece);
			piecesOnTheBoard.add(capturedPiece);
		}
		if (capturedPiece != null) {
			piecesOnTheBoard.remove(capturedPiece);
			capturedPieces.add(capturedPiece);
		}
		//movimento especial rei
		 if(p instanceof Rei && target.getColuna() == source.getColuna() + 2) {
			 Posicao sourceT = new Posicao(source.getLinha(), source.getColuna() + 3);
			 Posicao targetT = new Posicao(source.getLinha(), source.getColuna() + 1);
			 ChessPiece torre = (ChessPiece)board.removePiece(targetT);
			 board.placePiece(torre, sourceT);
			 torre.decreaseMoveCount();
			 
		 }
		 
			//movimento especial torre
		 if(p instanceof Rei && target.getColuna() == source.getColuna() - 2) {
			 Posicao sourceT = new Posicao(source.getLinha(), source.getColuna() - 4);
			 Posicao targetT = new Posicao(source.getLinha(), source.getColuna() - 1);
			 ChessPiece torre = (ChessPiece)board.removePiece(targetT);
			 board.placePiece(torre, sourceT);
			 torre.decreaseMoveCount();
			 
		 }
		 //Movimento especial
		 if(p instanceof Peao) {
			if(source.getColuna() != target.getColuna() && capturedPiece == enPassantVulnerable) {
				ChessPiece peao = (ChessPiece)board.removePiece(target);
				Posicao peaoPosicao;
				if(p.getColor() == Color.WHITE) {
					peaoPosicao = new Posicao(3, target.getColuna());
				}
				else{
					peaoPosicao = new Posicao(4, target.getColuna());
				}
				board.placePiece(peao, peaoPosicao);
				capturedPiece = board.removePiece(peaoPosicao);
				capturedPieces.add(capturedPiece);
				piecesOnTheBoard.remove(capturedPiece);
			}
		 }

		 
	}

	private void validateSourcePosition(Posicao posicao) {
		if (!board.thereIsAPiece(posicao)) {
			throw new ChessException("There is not piece on sours position");
		}
		if (currentPlayer != ((ChessPiece) board.piece(posicao)).getColor()) {
			throw new ChessException("The chose piece is not yours");
		}
		if (!board.piece(posicao).isThereAnyPossibleMove()) {
			throw new ChessException("There is no possible moves for the chosen piece");

		}
	}

	public void validateTargetPosition(Posicao source, Posicao target) {
		if (!board.piece(source).possibleMoves(target)) {
			throw new ChessException("The chosen piece can't move to target position");
		}
	}

	private void nextTurn() {
		turn++;
		currentPlayer = (currentPlayer == Color.WHITE) ? Color.BLACK : Color.WHITE;
	}

	private Color opponent(Color color) {
		return (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
	}

	private ChessPiece Rei(Color color) {
		List<Piece> list = piecesOnTheBoard.stream().filter(x -> ((ChessPiece) x).getColor() == color)
				.collect(Collectors.toList());
		for (Piece p : list) {
			if (p instanceof Rei) {
				return (ChessPiece) p;
			}
		}
		throw new IllegalStateException("There is no" + color + "King on the board");
	}

	private boolean testCheck(Color color) {
		Posicao ReiPosicao = Rei(color).getChessPosition().toPosition();
		List<Piece> opponentPieces = piecesOnTheBoard.stream()
				.filter(x -> ((ChessPiece) x).getColor() == opponent(color)).collect(Collectors.toList());
		for (Piece p : opponentPieces) {
			boolean[][] mat = p.possibleMoves();
			if (mat[ReiPosicao.getLinha()][ReiPosicao.getColuna()]) {
				return true;
			}
		}
		return false;

	}

	private boolean testCheckMate(Color color) {
		if (!testCheck(color)) {
			return false;
		}
		List<Piece> list = piecesOnTheBoard.stream().filter(x -> ((ChessPiece) x).getColor() == color)
				.collect(Collectors.toList());
		for (Piece p : list) {
			boolean[][] mat = p.possibleMoves();
			for (int i = 0; i < board.getLinhas(); i++) {
				for (int j = 0; j < board.getColunas(); j++) {
					if (mat[i][j]) {
						Posicao source = ((ChessPiece) p).getChessPosition().toPosition();
						Posicao target = new Posicao(i, j);
						Piece capturedPiece = makeMove(source, target);
						boolean testCheck = testCheck(color);
						undoMove(source, target, capturedPiece);
						if (!testCheck) {
							return false;
						}
					}
				}
			}

		}
		return true;
	}

	private void placeNewPiece(char coluna, int linha, ChessPiece piece) {
		board.placePiece(piece, new ChessPosition(coluna, linha).toPosition());
		piecesOnTheBoard.add(piece);
	}

	private void initialSetup() {
		
        placeNewPiece('a', 1, new Torre(board, Color.WHITE));
        placeNewPiece('b', 1, new Cavalo(board, Color.WHITE));
        placeNewPiece('c', 1, new Bispo(board, Color.WHITE));
        placeNewPiece('d', 1, new Rainha(board, Color.WHITE));
        placeNewPiece('e', 1, new Rei(board, Color.WHITE,this));
        placeNewPiece('f', 1, new Bispo(board, Color.WHITE));
        placeNewPiece('g', 1, new Cavalo(board, Color.WHITE));
        placeNewPiece('h', 1, new Torre(board, Color.WHITE));
        placeNewPiece('a', 2, new Peao(board, Color.WHITE,this));
        placeNewPiece('b', 2, new Peao(board, Color.WHITE,this));
        placeNewPiece('c', 2, new Peao(board, Color.WHITE,this));
        placeNewPiece('d', 2, new Peao(board, Color.WHITE,this));
        placeNewPiece('e', 2, new Peao(board, Color.WHITE,this));
        placeNewPiece('f', 2, new Peao(board, Color.WHITE,this));
        placeNewPiece('g', 2, new Peao(board, Color.WHITE,this));
        placeNewPiece('h', 2, new Peao(board, Color.WHITE,this));

        placeNewPiece('a', 8, new Torre(board, Color.BLACK));
        placeNewPiece('b', 8, new Cavalo(board, Color.BLACK));
        placeNewPiece('c', 8, new Bispo(board, Color.BLACK));
        placeNewPiece('d', 8, new Rainha(board, Color.BLACK));
        placeNewPiece('e', 8, new Rei(board, Color.BLACK,this));
        placeNewPiece('f', 8, new Bispo(board, Color.BLACK));
        placeNewPiece('g', 8, new Cavalo(board, Color.BLACK));
        placeNewPiece('h', 8, new Torre(board, Color.BLACK));
        placeNewPiece('a', 7, new Peao(board, Color.BLACK,this));
        placeNewPiece('b', 7, new Peao(board, Color.BLACK,this));
        placeNewPiece('c', 7, new Peao(board, Color.BLACK,this));
        placeNewPiece('d', 7, new Peao(board, Color.BLACK,this));
        placeNewPiece('e', 7, new Peao(board, Color.BLACK,this));
        placeNewPiece('f', 7, new Peao(board, Color.BLACK,this));
        placeNewPiece('g', 7, new Peao(board, Color.BLACK,this));
        placeNewPiece('h', 7, new Peao(board, Color.BLACK,this));
	}

}
