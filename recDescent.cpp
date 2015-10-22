#include <string>
using namespace std;

class TokenStream
{
public:
	TokenStream(string& input, int index) : input(input), index(index)
	{

	}

	char& Peek()
	{
		return input[index];
	}

	char& Read()
	{
		return input[index++];
	}

	bool IsNotEndOfInput()
	{
		return index < input.size();
	}

private:
	string& input;
	int index;
};

class Parser
{
public:

	static bool A(TokenStream& ts)
	{
		if(ts.IsNotEndOfInput())
		{
			switch(ts.Peek())
			{
			case 'a':
				return _Rule4(ts);
			case 'q':
				return _Rule5(ts);
			case 'b':
				return _Rule5(ts);
			case 'c':
				return _Rule5(ts);
			case '$':
				return _Rule5(ts);
			}
		}

		return false;
	}

	static bool Q(TokenStream& ts)
	{
		if(ts.IsNotEndOfInput())
		{
			switch(ts.Peek())
			{
			case 'q':
				return _Rule8(ts);
			case 'c':
				return _Rule9(ts);
			case '$':
				return _Rule9(ts);
			}
		}

		return false;
	}

	static bool B(TokenStream& ts)
	{
		if(ts.IsNotEndOfInput())
		{
			switch(ts.Peek())
			{
			case 'q':
				return _Rule7(ts);
			case 'b':
				return _Rule6(ts);
			case 'c':
				return _Rule7(ts);
			case 'd':
				return _Rule7(ts);
			case '$':
				return _Rule7(ts);
			}
		}

		return false;
	}

	static bool S(TokenStream& ts)
	{
		if(ts.IsNotEndOfInput())
		{
			switch(ts.Peek())
			{
			case 'a':
				return _Rule1(ts);
			case 'q':
				return _Rule1(ts);
			case 'b':
				return _Rule1(ts);
			case 'c':
				return _Rule1(ts);
			case '$':
				return _Rule1(ts);
			}
		}

		return false;
	}

	static bool C(TokenStream& ts)
	{
		if(ts.IsNotEndOfInput())
		{
			switch(ts.Peek())
			{
			case 'c':
				return _Rule2(ts);
			case 'd':
				return _Rule3(ts);
			case '$':
				return _Rule3(ts);
			}
		}

		return false;
	}

	static bool _Rule1(TokenStream& ts)
	{
		if(A(ts) && C(ts) && ts.IsNotEndOfInput() && ts.Read() == '$')
		{
			return true;
		}

		return false;
	}

	static bool _Rule2(TokenStream& ts)
	{
		if(ts.IsNotEndOfInput() && ts.Read() == 'c')
		{
			return true;
		}

		return false;
	}

	static bool _Rule3(TokenStream& ts)
	{
		return true;
	}

	static bool _Rule4(TokenStream& ts)
	{
		if(ts.IsNotEndOfInput() && ts.Read() == 'a' && B(ts) && C(ts) && ts.IsNotEndOfInput() && ts.Read() == 'd')
		{
			return true;
		}

		return false;
	}

	static bool _Rule5(TokenStream& ts)
	{
		if(B(ts) && Q(ts))
		{
			return true;
		}

		return false;
	}

	static bool _Rule6(TokenStream& ts)
	{
		if(ts.IsNotEndOfInput() && ts.Read() == 'b' && B(ts))
		{
			return true;
		}

		return false;
	}

	static bool _Rule7(TokenStream& ts)
	{
		return true;
	}

	static bool _Rule8(TokenStream& ts)
	{
		if(ts.IsNotEndOfInput() && ts.Read() == 'q')
		{
			return true;
		}

		return false;
	}

	static bool _Rule9(TokenStream& ts)
	{
		return true;
	}

};

bool Parse(string& input)
{
	TokenStream* ts = new TokenStream(input, 0);

	if(ts->IsNotEndOfInput())
	{
		if(Parser::S(*ts) && !ts->IsNotEndOfInput())
		{
			return true;
		}
	}

	return false;
}