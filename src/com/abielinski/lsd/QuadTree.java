package com.abielinski.lsd;

import java.util.ArrayList;

import com.abielinski.lsd.util.Rectangle;

public class QuadTree extends Rectangle {
	/**
	 * Flag for specifying that you want to add an object to the A list.
	 */
	static public final int A_LIST = 0;
	/**
	 * Flag for specifying that you want to add an object to the B list.
	 */
	static public final int B_LIST = 1;
	
	/**
	 * Whether this branch of the tree can be subdivided or not.
	 */
	protected boolean _canSubdivide;
	
	/**
	 * These variables refer to the internal A and B linked lists,
	 * which are used to store objects in the leaves.
	 */
	protected ArrayList _headA;
	protected ArrayList _tailA;
	protected ArrayList _headB;
	protected ArrayList _tailB;

	/**
	 * These variables refer to the potential child quadrants for this node.
	 */
	static protected int _min;
	protected QuadTree _nw;
	protected QuadTree _ne;
	protected QuadTree _se;
	protected QuadTree _sw;		
	protected float _l;
	protected float _r;
	protected float _t;
	protected float _b;
	protected float _hw;
	protected float _hh;
	protected float _mx;
	protected float _my;
	
	/**
	 * These objects are used to reduce recursive parameters internally.
	 */
	static protected LSDSprite _o;
	static protected float _ol;
	static protected float _ot;
	static protected float _or;
	static protected float _ob;
	static protected int _oa;
	static protected Object _oc;
	
	/**
	 * Instantiate a new Quad Tree node.
	 * 
	 * @param	X			The X-coordinate of the point in space.
	 * @param	Y			The Y-coordinate of the point in space.
	 * @param	Width		Desired width of this node.
	 * @param	Height		Desired height of this node.
	 * @param	Parent		The parent branch or node.  Pass null to create a root.
	 */
	public QuadTree(float X, float Y, float Width, float Height, QuadTree Parent)
	{
		super(X,Y,Width,Height);
		_headA = _tailA = new ArrayList();
		_headB = _tailB = new ArrayList();
		
		/* DEBUG draw a randomly colored rectangle indicating this quadrant (may induce seizures)
		FlxSprite brush = new FlxSprite().createGraphic(Width,Height,0xffffffff*FlxU.random());
		FlxState.screen.draw(brush,X+FlxG.scroll.x,Y+FlxG.scroll.y);//*/
		
		//Copy the parent's children (if there are any)
		if(Parent != null)
		{
			ArrayList itr;
			ArrayList ot;
			if(Parent._headA.object != null)
			{
				itr = Parent._headA;
				while(itr != null)
				{
					if(_tailA.object != null)
					{
						ot = _tailA;
						_tailA = new ArrayList();
						ot.next = _tailA;
					}
					_tailA.object = itr.object;
					itr = itr.next;
				}
			}
			if(Parent._headB.object != null)
			{
				itr = Parent._headB;
				while(itr != null)
				{
					if(_tailB.object != null)
					{
						ot = _tailB;
						_tailB = new ArrayList();
						ot.next = _tailB;
					}
					_tailB.object = itr.object;
					itr = itr.next;
				}
			}
		}
		else
			_min = (width + height)/(2*FlxU.quadTreeDivisions);
		_canSubdivide = (width > _min) || (height > _min);
		
		//Set up comparison/sort helpers
		_nw = null;
		_ne = null;
		_se = null;
		_sw = null;
		_l = x;
		_r = x+width;
		_hw = width/2;
		_mx = _l+_hw;
		_t = y;
		_b = y+height;
		_hh = height/2;
		_my = _t+_hh;
	}
	
	/**
	 * Call this to add an object to the root of the tree.
	 * This will recursively add all group members, but
	 * not the groups themselves.
	 * 
	 * @param	Object		The <code>LSDSprite</code> you want to add.  <code>FlxGroup</code> objects will be recursed and their applicable members added automatically.
	 * @param	List		A <code>int</code> flag indicating the list to which you want to add the objects.  Options are <code>A_LIST</code> and <code>B_LIST</code>.
	 */
	public void add(LSDSprite Object, int List) 
	{
		_oa = List;
		if(Object._group)
		{
			LSDSprite m;
			Array members = (Object as FlxGroup).members;
			int l = members.length;
			for(int i = 0; i < l; i++)
			{
				m = members[i] as LSDSprite;
				if((m != null) && m.exists)
				{
					if(m._group)
						add(m,List);
					else if(m.solid)
					{
						_o = m;
						_ol = _o.x;
						_ot = _o.y;
						_or = _o.x + _o.width;
						_ob = _o.y + _o.height;
						addObject();
					}
				}
			}
		}
		if(Object.solid)
		{
			_o = Object;
			_ol = _o.x;
			_ot = _o.y;
			_or = _o.x + _o.width;
			_ob = _o.y + _o.height;
			addObject();
		}
	}
	
	/**
	 * Internal for recursively navigating and creating the tree
	 * while adding objects to the appropriate nodes.
	 */
	protected void addObject() 
	{
		//If this quad (not its children) lies entirely inside this object, add it here
		if(!_canSubdivide || ((_l >= _ol) && (_r <= _or) && (_t >= _ot) && (_b <= _ob)))
		{
			addToList();
			return;
		}
		
		//See if the selected object fits completely inside any of the quadrants
		if((_ol > _l) && (_or < _mx))
		{
			if((_ot > _t) && (_ob < _my))
			{
				if(_nw == null)
					_nw = new QuadTree(_l,_t,_hw,_hh,this);
				_nw.addObject();
				return;
			}
			if((_ot > _my) && (_ob < _b))
			{
				if(_sw == null)
					_sw = new QuadTree(_l,_my,_hw,_hh,this);
				_sw.addObject();
				return;
			}
		}
		if((_ol > _mx) && (_or < _r))
		{
			if((_ot > _t) && (_ob < _my))
			{
				if(_ne == null)
					_ne = new QuadTree(_mx,_t,_hw,_hh,this);
				_ne.addObject();
				return;
			}
			if((_ot > _my) && (_ob < _b))
			{
				if(_se == null)
					_se = new QuadTree(_mx,_my,_hw,_hh,this);
				_se.addObject();
				return;
			}
		}
		
		//If it wasn't completely contained we have to check out the partial overlaps
		if((_or > _l) && (_ol < _mx) && (_ob > _t) && (_ot < _my))
		{
			if(_nw == null)
				_nw = new QuadTree(_l,_t,_hw,_hh,this);
			_nw.addObject();
		}
		if((_or > _mx) && (_ol < _r) && (_ob > _t) && (_ot < _my))
		{
			if(_ne == null)
				_ne = new QuadTree(_mx,_t,_hw,_hh,this);
			_ne.addObject();
		}
		if((_or > _mx) && (_ol < _r) && (_ob > _my) && (_ot < _b))
		{
			if(_se == null)
				_se = new QuadTree(_mx,_my,_hw,_hh,this);
			_se.addObject();
		}
		if((_or > _l) && (_ol < _mx) && (_ob > _my) && (_ot < _b))
		{
			if(_sw == null)
				_sw = new QuadTree(_l,_my,_hw,_hh,this);
			_sw.addObject();
		}
	}
	
	/**
	 * Internal for recursively adding objects to leaf lists.
	 */
	protected void addToList() 
	{
		ArrayList ot;
		if(_oa == A_LIST)
		{
			if(_tailA.object != null)
			{
				ot = _tailA;
				_tailA = new ArrayList();
				ot.next = _tailA;
			}
			_tailA.object = _o;
		}
		else
		{
			if(_tailB.object != null)
			{
				ot = _tailB;
				_tailB = new ArrayList();
				ot.next = _tailB;
			}
			_tailB.object = _o;
		}
		if(!_canSubdivide)
			return;
		if(_nw != null)
			_nw.addToList();
		if(_ne != null)
			_ne.addToList();
		if(_se != null)
			_se.addToList();
		if(_sw != null)
			_sw.addToList();
	}
	
	/**
	 * <code>QuadTree</code>'s other main function.  Call this after adding objects
	 * using <code>QuadTree.add()</code> to compare the objects that you loaded.
	 * 
	 * @param	BothLists	Whether you are doing an A-B list comparison, or comparing A against itself.
	 * @param	Callback	A with two <code>LSDSprite</code> parameters - e.g. <code>myOverlapFunction(LSDSprite Object1,LSDSprite Object2);</code>  If no is provided, <code>QuadTree</code> will call <code>kill()</code> on both objects.
	 *
	 * @return	Whether or not any overlaps were found.
	 */
	public Boolean overlap(Boolean BothLists=true,Callback=null) 
	{
		_oc = Callback;
		Boolean c = false;
		ArrayList itr;
		if(BothLists)
		{
			//An A-B list comparison
			_oa = B_LIST;
			if(_headA.object != null)
			{
				itr = _headA;
				while(itr != null)
				{
					_o = itr.object;
					if(_o.exists && _o.solid && overlapNode())
						c = true;
					itr = itr.next;
				}
			}
			_oa = A_LIST;
			if(_headB.object != null)
			{
				itr = _headB;
				while(itr != null)
				{
					_o = itr.object;
					if(_o.exists && _o.solid)
					{
						if((_nw != null) && _nw.overlapNode())
							c = true;
						if((_ne != null) && _ne.overlapNode())
							c = true;
						if((_se != null) && _se.overlapNode())
							c = true;
						if((_sw != null) && _sw.overlapNode())
							c = true;
					}
					itr = itr.next;
				}
			}
		}
		else
		{
			//Just checking the A list against itself
			if(_headA.object != null)
			{
				itr = _headA;
				while(itr != null)
				{
					_o = itr.object;
					if(_o.exists && _o.solid && overlapNode(itr.next))
						c = true;
					itr = itr.next;
				}
			}
		}
		
		//Advance through the tree by calling overlap on each child
		if((_nw != null) && _nw.overlap(BothLists,_oc))
			c = true;
		if((_ne != null) && _ne.overlap(BothLists,_oc))
			c = true;
		if((_se != null) && _se.overlap(BothLists,_oc))
			c = true;
		if((_sw != null) && _sw.overlap(BothLists,_oc))
			c = true;
		
		return c;
	}
	
	/**
	 * An internal for comparing an object against the contents of a node.
	 * 
	 * @param	Iterator	An optional pointer to a linked list entry (for comparing A against itself).
	 * 
	 * @return	Whether or not any overlaps were found.
	 */
	protected Boolean overlapNode(ArrayList Iterator=null) 
	{
		//member list setup
		Boolean c = false;
		LSDSprite co;
		ArrayList itr = Iterator;
		if(itr == null)
		{
			if(_oa == A_LIST)
				itr = _headA;
			else
				itr = _headB;
		}
		
		//Make sure this is a valid list to walk first!
		if(itr.object != null)
		{
			//Walk the list and check for overlaps
			while(itr != null)
			{
				co = itr.object;
				if( (_o === co) || !co.exists || !_o.exists || !co.solid || !_o.solid ||
					(_o.x + _o.width  < co.x + FlxU.roundingError) ||
					(_o.x + FlxU.roundingError > co.x + co.width) ||
					(_o.y + _o.height < co.y + FlxU.roundingError) ||
					(_o.y + FlxU.roundingError > co.y + co.height) )
				{
					itr = itr.next;
					continue;
				}
				if(_oc == null)
				{
					_o.kill();
					co.kill();
					c = true;
				}
				else if(_oc(_o,co))
					c = true;
				itr = itr.next;
			}
		}
		
		return c;
	}

}
