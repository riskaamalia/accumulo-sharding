--------------------------------------------------------------------------------
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------


The conversation so far has been simple. But 
That sounds pretty 
simple but the "key" itself is complex. It has several components:

-------------------------------------------------------------------  ---------
| row | column family | column qualifier | visibility | timestamp |  | value |
-------------------------------------------------------------------  ---------

These five components, combined, go into the "key". For the our current 
conversation about shards, it's the row value that is important because rows 
won't be split between tablets. For our purposes, we can consider a tablet to 
be the equivalent of a cluster node. As you work with Accumulo in more depth, 
you'll refine your idea of a tablet but let's keep it simple. One tablet is 
one node.

To review, a shard is a partition is a tablet is a node is a server --- for 
the moment.

Simplifying even farther we just look at the row values. And use examples to 
see how rows behave.


When an Accumulo table is created, 

It may be tempting to have the computers flip a virtual coin to decide which
server to target for each record. In the RDBMS world that procedure works but
in key-value databases, information is stored vertically instead of 
horizontally so the coin flip analogy does not work. Let's quickly review why.

Coin Flip Sharding

Relatonal databases spread information across columms (i.e., horizontally). 
Each record has fields that conceptually are laid out like this. The Id value 
is a synthetic key (SK) and I hope you have them in your data. If not your very 
first task is to get your DBA's to add  them. Seriously, synthetic keys save 
you a world of future trouble. Here is a simple relational record.

|--------------------------------------
| SK   | First Name | Last Name | Age |
|-------------------------------------|
| 1001 | John       | Kloplick  | 36  |
---------------------------------------

Key-value database spread information across several rows using the synthetic
key to tie them together. In vastly simplified form, the information is stored
in three key-value combinations (or three entries).

|----------------------------------
| SK   | Field Name | Field Value |
|---------------------------------|
| 1001 | first_name | John        |
| 1001 | last_name  | Kloplick    |
| 1001 | age        | 36          |
-----------------------------------

If the coin flip sharding strategy were used the information might spread
acrosss a two node cluster like this:

|------------------------------------------
| Shard | SK   | Field Name | Field Value |
|-----------------------------------------|
| 1     | 1001 | first_name | John        |
| 2     | 1001 | last_name  | Kloplick    |
| 1     | 1001 | age        | 36          |
-------------------------------------------

To retreve the information you'd need to scan both servers! This coin flip
sharding technique is not going to scale. Imagine information about a person
spread over 40 servers. Collating that information would be prohibitively 
time-consuming.

HASH + MOD Sharding (using first_name field)

Of course, there is a better sharding strategy to use. You can base the
strategy on one of the fields. Get its hash code and then mod it by the
number of partitions. Ultimately, this strategy will fail but let's go 
through the process to see why. Skip to the next section if you already see
the problem.

"John".hashCode() is 2314539. Then we can mod that by the number of 
partitions (or servers) in our cluster. Let's pretend we have 5 servers 
instead of the two we used earlier for variety. Our key-value entries now
look thusly:

|------------------------------------------
| Shard | SK   | Field Name | Field Value |
|-----------------------------------------|
| 4     | 1001 | first_name | John        |
| 4     | 1001 | last_name  | Kloplick    |
| 4     | 1001 | age        | 36          |
-------------------------------------------

It's time to look at a specific use case to see if this sharding strategy
is sound. What if we need to add a set of friends for John? It's highly
unlikely that the information about John's friends have his first name. But 
very likely for his synthetic key of 1001 to be there. We can now see choosing
the first_name field as the base of the sharding strategy was unwise.

HASH + MOD Sharding (using synthetic key)

Using the synthetic key as the basis for the hash provides more continuity
between updates. And regardless of what information changes, we'll always
put the information in the same shard. A small speed improvement happens because
the synthetic key will most likely be a number so we don't need to execute
the hashcode method. "1001 % 5" (or the remainder of 1001 divided by 5) is 1.
So the key-value information is now:

|------------------------------------------
| Shard | SK   | Field Name | Field Value |
|-----------------------------------------|
| 1     | 1001 | first_name | John        |
| 1     | 1001 | last_name  | Kloplick    |
| 1     | 1001 | age        | 36          |
-------------------------------------------

Because the shard was different when adding the changed information, the 
first_name entry is added to 

the synthetic key (or whatever set of fields generates a primary
key).


However, once you go beyond the high-level abstraction the data details can 
turn that nice 

 Considering the amount of data in question, it
is impossible to manually select the target server for each entry.

Consultants
-----------
Walt Stoneburner
Billie Rinaldi
